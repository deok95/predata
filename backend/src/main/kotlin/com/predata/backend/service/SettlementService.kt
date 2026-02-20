package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Member
import com.predata.backend.domain.PoolStatus
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.VotingPhase
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.settlement.SettlementCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class SettlementService(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val tierService: TierService,
    private val rewardService: RewardService,
    private val blockchainService: BlockchainService,
    private val badgeService: BadgeService,
    private val transactionHistoryService: TransactionHistoryService,
    private val settlementCalculator: SettlementCalculator,
    private val resolutionAdapterRegistry: com.predata.backend.service.settlement.adapters.ResolutionAdapterRegistry,
    private val auditService: AuditService,
    private val feePoolService: FeePoolService,
    private val voteRewardDistributionService: VoteRewardDistributionService,
    private val questionLifecycleService: QuestionLifecycleService,
    private val transactionManager: PlatformTransactionManager,
    private val userSharesRepository: com.predata.backend.repository.amm.UserSharesRepository,
    private val marketPoolRepository: com.predata.backend.repository.amm.MarketPoolRepository
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SettlementService::class.java)

    private val requiresNewTx by lazy {
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
    }

    companion object {
        private const val DISPUTE_HOURS = 0L // 테스트용: 이의제기 기간 없음 (즉시 확정)
        // TODO: 실제 운영에서는 24L로 변경
        private const val AUTO_SETTLE_MIN_CONFIDENCE = 0.99
    }

    /**
     * 자동정산 Auto-first + Fail-safe 구현
     * 7가지 조건 체크:
     * 1. marketType == VERIFIABLE
     * 2. result, sourcePayload, sourceUrl, confidence 모두 non-null
     * 3. confidence >= 0.99
     * 4. 소스가 최종 상태 확인 (경기 FINISHED / 종가 확정)
     * 5. 30초 간격 2회 조회 일치 (TODO)
     * 6. idempotency: questionId + result + sourcePayloadHash로 중복 정산 방지 (TODO)
     * 7. AuditLog에 정산 이벤트 기록 후 finalize
     */
    @Transactional
    fun autoSettleWithVerification(questionId: Long): SettlementResult? {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        // 1. marketType == VERIFIABLE 체크
        if (question.marketType != com.predata.backend.domain.MarketType.VERIFIABLE) {
            logger.info("[AutoSettle] 질문 #{} VERIFIABLE이 아님, 수동 정산 대기", questionId)
            return null
        }

        // ResolutionAdapter를 통해 결과 조회
        val resolutionResult = try {
            resolutionAdapterRegistry.resolve(question)
        } catch (e: Exception) {
            logger.error("[AutoSettle] Question #{} result fetch failed: {}", questionId, e.message)
            return null
        }

        // 2. result, sourcePayload, sourceUrl, confidence 모두 non-null 체크
        val result = resolutionResult.result
        val sourcePayload = resolutionResult.sourcePayload
        val sourceUrl = resolutionResult.sourceUrl
        val confidence = resolutionResult.confidence

        if (result == null || sourcePayload == null || sourceUrl == null || confidence == null) {
            logger.info("[AutoSettle] 질문 #{} 결과 불완전 (result={}, confidence={}), 재시도 대기", questionId, result, confidence)
            return null
        }

        // 3. confidence >= 0.99 체크
        if (confidence < AUTO_SETTLE_MIN_CONFIDENCE) {
            logger.info("[AutoSettle] 질문 #{} confidence 부족 ({}), 수동 정산 대기", questionId, confidence)
            return null
        }

        // 4. 소스가 최종 상태 확인 (sourcePayload에서 status 체크)
        if (!sourcePayload.contains("FINISHED")) {
            logger.info("[AutoSettle] 질문 #{} 소스 미확정 상태, 재시도 대기", questionId)
            return null
        }

        // TODO: 5. 30초 간격 2회 조회 일치 (캐시 구현 필요)
        // TODO: 6. idempotency 체크 (중복 정산 방지)

        // 7. AuditLog 기록 및 자동 정산 실행
        logger.info("[AutoSettle] 질문 #{} 자동 정산 조건 충족, finalize 실행", questionId)
        auditService.log(
            memberId = null,
            action = com.predata.backend.domain.AuditAction.SETTLE,
            entityType = "QUESTION",
            entityId = questionId,
            detail = "Auto-settlement verified: ${result.name}, confidence=$confidence"
        )

        // 정산 시작
        initiateSettlement(questionId, result, sourceUrl)

        // 즉시 확정
        return finalizeSettlement(questionId, skipDeadlineCheck = true)
    }

    /**
     * 1단계: 정산 시작 (자동 정산 - 어댑터 사용)
     * ResolutionAdapterRegistry를 통해 자동으로 결과를 결정한다.
     */
    @Transactional
    fun initiateSettlementAuto(questionId: Long): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("Question already settled.")
        }
        if (question.status != QuestionStatus.VOTING && question.status != QuestionStatus.BETTING && question.status != QuestionStatus.BREAK) {
            throw IllegalStateException("Question status is not eligible for settlement. (current: ${question.status})")
        }

        // ResolutionAdapter를 통해 자동 정산
        val resolutionResult = resolutionAdapterRegistry.resolve(question)

        // 결과가 미확정이면 정산 시작 불가
        val finalResult = resolutionResult.result
            ?: throw IllegalStateException("Settlement result not yet confirmed.")
        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("Settlement result not yet confirmed.")
        }

        // 기존 메서드 재사용
        return initiateSettlement(questionId, finalResult, resolutionResult.sourceUrl)
    }

    /**
     * 1단계: 정산 시작 (PENDING_SETTLEMENT)
     * 결과와 근거를 기록하고 이의 제기 기간을 시작한다.
     * 배당 분배는 아직 하지 않는다.
     */
    @Transactional
    fun initiateSettlement(questionId: Long, finalResult: FinalResult, sourceUrl: String?): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("Question already settled.")
        }
        if (question.status != QuestionStatus.VOTING && question.status != QuestionStatus.BETTING && question.status != QuestionStatus.BREAK) {
            throw IllegalStateException("Question status is not eligible for settlement. (current: ${question.status})")
        }

        question.status = QuestionStatus.SETTLED
        question.finalResult = finalResult
        question.sourceUrl = sourceUrl
        // 이의제기 기간이 0이면 즉시 확정 가능하도록 과거 시간 설정
        question.disputeDeadline = if (DISPUTE_HOURS == 0L) {
            LocalDateTime.now().minusSeconds(1)
        } else {
            LocalDateTime.now().plusHours(DISPUTE_HOURS)
        }
        questionRepository.save(question)

        // Audit log: 정산 시작
        auditService.log(
            memberId = null,
            action = com.predata.backend.domain.AuditAction.SETTLE,
            entityType = "QUESTION",
            entityId = questionId,
            detail = "Settlement initiated: ${finalResult.name}"
        )

        // AMM_FPMM 정산 대상 확인
        marketPoolRepository.findById(questionId).orElseThrow {
            IllegalStateException("AMM 마켓 풀이 없습니다.")
        }
        val totalBets = userSharesRepository.findByQuestionId(questionId).size

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = totalBets,
            totalWinners = 0,
            totalPayout = 0,
            voterRewards = 0,
            message = "Settlement initiated. Will be finalized after ${DISPUTE_HOURS} hour(s) dispute period."
        )
    }

    /**
     * 2단계: 정산 확정 (SETTLED)
     * 이의 제기 기간이 지난 후 배당금 분배를 실행한다.
     * 관리자가 강제 확정할 경우 skipDeadlineCheck = true
     *
     * 최적화: 3개 서비스(Settlement/Tier/Reward) 로직을 통합
     * - 1회 조회 → 메모리 처리 → 1회 저장
     */
    @Transactional
    fun finalizeSettlement(questionId: Long, skipDeadlineCheck: Boolean = false): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        if (question.status != QuestionStatus.SETTLED) {
            throw IllegalStateException("Only pending settlement questions can be finalized. (current: ${question.status})")
        }

        // 이중 정산 방지: disputeDeadline이 null이면 이미 finalize 완료된 질문
        if (question.disputeDeadline == null) {
            throw IllegalStateException("Settlement already finalized.")
        }

        if (!skipDeadlineCheck && LocalDateTime.now().isBefore(question.disputeDeadline)) {
            throw IllegalStateException("Dispute period has not ended yet. (deadline: ${question.disputeDeadline})")
        }

        return finalizeAmmSettlement(question, skipDeadlineCheck)
    }

    /**
     * AMM (FPMM) 정산 로직
     * user_shares 기반으로 정산하고, 기존 오더북 로직과 완전히 분리됨
     */
    @Transactional
    private fun finalizeAmmSettlement(question: Question, skipDeadlineCheck: Boolean): SettlementResult {
        val questionId = question.id ?: throw IllegalStateException("Question ID is null")
        val finalResult = question.finalResult

        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("Cannot finalize settlement without confirmed final result.")
        }

        // 1) 결과 확인 - ShareOutcome으로 변환
        val winningSide: ShareOutcome? = when (finalResult) {
            FinalResult.YES -> ShareOutcome.YES
            FinalResult.NO -> ShareOutcome.NO
            // TODO: FinalResult enum에 DRAW, CANCELLED 추가 후 구현
            // FinalResult.DRAW -> null
            // FinalResult.CANCELLED -> null
            else -> throw IllegalStateException("Unsupported result: $finalResult")
        }

        // 2) 해당 question의 모든 user_shares 조회
        val allShares = userSharesRepository.findByQuestionId(questionId)

        // 배치 조회: N+1 → 1회
        val memberIds = allShares.map { it.memberId }.distinct()
        val membersMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }

        var totalWinners = 0
        var totalPayout = BigDecimal.ZERO
        val updatedMembers = mutableListOf<Member>()

        // 3) 정산 실행
        allShares.forEach { userShare ->
            val member = membersMap[userShare.memberId] ?: return@forEach

            when {
                // 승패가 있는 경우
                winningSide != null -> {
                    if (userShare.outcome == winningSide) {
                        // 승자: 1 share = 1 USDC (수수료 없음, 스왑 시 이미 징수)
                        val payout = userShare.shares.setScale(18, RoundingMode.DOWN)
                        member.usdcBalance = member.usdcBalance.add(payout)
                        updatedMembers.add(member)

                        // TransactionHistory 기록
                        transactionHistoryService.record(
                            memberId = userShare.memberId,
                            type = "AMM_SETTLEMENT",
                            amount = payout,
                            balanceAfter = member.usdcBalance,
                            description = "AMM 정산 승리 - Question #$questionId",
                            questionId = questionId
                        )

                        // Activity 기록
                        activityRepository.save(
                            Activity(
                                memberId = userShare.memberId,
                                questionId = questionId,
                                activityType = ActivityType.BET,
                                choice = if (userShare.outcome == ShareOutcome.YES) Choice.YES else Choice.NO,
                                amount = payout.toLong()
                            )
                        )

                        totalWinners++
                        totalPayout = totalPayout.add(payout)
                    }
                    // 패자: 아무것도 안 함 (shares 가치 = 0)
                }

                // TODO: DRAW 처리 (FinalResult enum 확장 필요)
                // question.finalResult == FinalResult.DRAW -> {
                //     val pool = marketPoolRepository.findById(questionId).orElse(null)
                //     if (pool != null) {
                //         val price = com.predata.backend.service.amm.FpmmMathEngine.calculatePrice(pool.yesShares, pool.noShares)
                //         val sharePrice = if (userShare.outcome == ShareOutcome.YES) price.pYes else price.pNo
                //         val refund = userShare.shares.multiply(sharePrice).setScale(18, RoundingMode.DOWN)
                //         member.usdcBalance = member.usdcBalance.add(refund)
                //     }
                // }

                // TODO: CANCELLED 처리 (FinalResult enum 확장 필요)
                // question.finalResult == FinalResult.CANCELLED -> {
                //     val refund = userShare.costBasisUsdc
                //     member.usdcBalance = member.usdcBalance.add(refund)
                // }
            }

            // shares를 0으로 마킹 (삭제하지 않음)
            userShare.shares = BigDecimal.ZERO
            userShare.costBasisUsdc = BigDecimal.ZERO
            userSharesRepository.save(userShare)
        }

        // 일괄 저장
        memberRepository.saveAll(updatedMembers)

        // 4) 풀 상태 변경 및 collateralLocked 차감
        val pool = marketPoolRepository.findById(questionId).orElse(null)
        if (pool != null) {
            val newLocked = pool.collateralLocked.subtract(totalPayout)
                .setScale(18, RoundingMode.DOWN)
            require(newLocked >= BigDecimal.ZERO) {
                "Insolvency error: payout=$totalPayout > collateralLocked=${pool.collateralLocked}"
            }
            pool.collateralLocked = newLocked
            pool.status = PoolStatus.SETTLED
            marketPoolRepository.save(pool)
        }

        // 5) 질문 상태 변경 및 disputeDeadline null로 설정 (이중 정산 방지)
        question.status = QuestionStatus.SETTLED
        question.disputeDeadline = null
        questionRepository.save(question)

        // 온체인 기록
        blockchainService.settleQuestionOnChain(questionId, finalResult)

        // 투표 참여자 보상 분배 (수수료 리워드 풀에서) — REQUIRES_NEW로 외부 트랜잭션 격리
        var voterRewardsAmount = 0L
        try {
            val rewardResult = requiresNewTx.execute {
                voteRewardDistributionService.distributeRewards(questionId)
            }
            val distributed = rewardResult?.get("totalAmount")
            if (distributed is BigDecimal) {
                voterRewardsAmount = distributed.setScale(0, RoundingMode.DOWN).toLong()
            }
            logger.info("Vote reward distribution completed: questionId=$questionId, result=$rewardResult")
        } catch (e: Exception) {
            // 투표 보상 실패가 정산 자체를 롤백하면 안 됨
            logger.error("Vote reward distribution failed (non-blocking): questionId=$questionId, error=${e.message}", e)
        }

        // 투표 참여자 보상 분배 성공 시 VotingPhase 전환 — REQUIRES_NEW로 외부 트랜잭션 격리
        if (voterRewardsAmount > 0) {
            try {
                requiresNewTx.execute {
                    questionLifecycleService.transitionTo(questionId, VotingPhase.REWARD_DISTRIBUTED)
                }
            } catch (e: Exception) {
                logger.error("Phase transition to REWARD_DISTRIBUTED failed (non-blocking): questionId=$questionId", e)
            }
        }

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = allShares.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout.setScale(0, RoundingMode.DOWN).toLong(),
            voterRewards = voterRewardsAmount,
            message = "AMM settlement finalized."
        )
    }

    /**
     * 정산 취소 (PENDING_SETTLEMENT → OPEN)
     * 이의 제기로 인해 정산을 취소하고 질문을 다시 열린 상태로 되돌린다.
     */
    @Transactional
    fun cancelPendingSettlement(questionId: Long): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        if (question.status != QuestionStatus.SETTLED) {
            throw IllegalStateException("Only pending settlement questions can be cancelled. (current: ${question.status})")
        }

        // 만료일 체크: 이미 만료된 질문은 SETTLED로, 아니면 BETTING으로 복원
        val newStatus = if (question.expiredAt.isBefore(LocalDateTime.now())) QuestionStatus.SETTLED else QuestionStatus.BETTING

        question.status = newStatus
        question.finalResult = FinalResult.PENDING
        question.sourceUrl = null
        question.disputeDeadline = null
        questionRepository.save(question)

        // Audit log: 정산 취소
        auditService.log(
            memberId = null,
            action = com.predata.backend.domain.AuditAction.CANCEL,
            entityType = "QUESTION",
            entityId = questionId,
            detail = "Settlement cancelled, status restored to ${newStatus.name}"
        )

        return SettlementResult(
            questionId = questionId,
            finalResult = "CANCELLED",
            totalBets = 0,
            totalWinners = 0,
            totalPayout = 0,
            voterRewards = 0,
            message = if (newStatus == QuestionStatus.SETTLED)
                "Settlement cancelled. Question has expired and transitioned to SETTLED status."
            else
                "Settlement cancelled. Question has been restored to BETTING status."
        )
    }

    /**
     * 사용자별 정산 내역 조회 (Activity 기반)
     */
    fun getSettlementHistory(memberId: Long): List<SettlementHistoryItem> {
        val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)

        // 배치 조회: N+1 → 1회
        val questionIds = bets.map { it.questionId }.distinct()
        val questionsMap = questionRepository.findAllById(questionIds).associateBy { it.id!! }

        return bets.mapNotNull { bet ->
            val question = questionsMap[bet.questionId]
            if (question == null || question.status != QuestionStatus.SETTLED) return@mapNotNull null

            val winningChoice = when (question.finalResult) {
                FinalResult.YES -> Choice.YES
                FinalResult.NO -> Choice.NO
                FinalResult.PENDING -> return@mapNotNull null  // 미확정은 히스토리에서 제외
            }
            val isWinner = bet.choice == winningChoice
            val payout = if (isWinner) bet.amount else 0L

            SettlementHistoryItem(
                questionId = question.id ?: 0,
                questionTitle = question.title,
                myChoice = bet.choice.name,
                finalResult = question.finalResult.name,
                betAmount = bet.amount,
                payout = payout,
                profit = payout - bet.amount,
                isWinner = isWinner
            )
        }
    }
}

/**
 * 정산 결과 DTO
 */
data class SettlementResult(
    val questionId: Long,
    val finalResult: String,
    val totalBets: Int,
    val totalWinners: Int,
    val totalPayout: Long,
    val voterRewards: Long,
    val message: String
)

/**
 * 정산 내역 DTO
 */
data class SettlementHistoryItem(
    val questionId: Long,
    val questionTitle: String,
    val myChoice: String,
    val finalResult: String,
    val betAmount: Long,
    val payout: Long,
    val profit: Long,
    val isWinner: Boolean
)
