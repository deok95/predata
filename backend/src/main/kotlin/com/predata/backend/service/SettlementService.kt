package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.ExecutionModel
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.PoolStatus
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.settlement.SettlementPolicyFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val settlementPolicyFactory: SettlementPolicyFactory,
    private val positionService: PositionService,
    private val resolutionAdapterRegistry: com.predata.backend.service.settlement.adapters.ResolutionAdapterRegistry,
    private val auditService: AuditService,
    private val feePoolService: FeePoolService,
    private val voteRewardDistributionService: VoteRewardDistributionService,
    private val userSharesRepository: com.predata.backend.repository.amm.UserSharesRepository,
    private val marketPoolRepository: com.predata.backend.repository.amm.MarketPoolRepository
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SettlementService::class.java)

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

        // ExecutionModel에 따라 정산 대상 확인
        val totalBets = when (question.executionModel) {
            ExecutionModel.ORDERBOOK_LEGACY -> {
                val positions = positionService.getPositionsByQuestionId(questionId)
                if (positions.isEmpty()) {
                    throw IllegalStateException("No positions to settle.")
                }
                positions.size
            }
            ExecutionModel.AMM_FPMM -> {
                marketPoolRepository.findById(questionId).orElseThrow {
                    IllegalStateException("AMM 마켓 풀이 없습니다.")
                }
                userSharesRepository.findByQuestionId(questionId).size
            }
        }

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

        // execution_model에 따라 분기
        return when (question.executionModel) {
            ExecutionModel.AMM_FPMM -> finalizeAmmSettlement(question, skipDeadlineCheck)
            ExecutionModel.ORDERBOOK_LEGACY -> finalizeOrderbookSettlement(question, skipDeadlineCheck)
        }
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

        var totalWinners = 0
        var totalPayout = BigDecimal.ZERO

        // 3) 정산 실행
        allShares.forEach { userShare ->
            val member = memberRepository.findById(userShare.memberId).orElse(null) ?: return@forEach

            when {
                // 승패가 있는 경우
                winningSide != null -> {
                    if (userShare.outcome == winningSide) {
                        // 승자: 1 share = 1 USDC (수수료 없음, 스왑 시 이미 징수)
                        val payout = userShare.shares.setScale(18, RoundingMode.DOWN)
                        member.usdcBalance = member.usdcBalance.add(payout)

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

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = allShares.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout.setScale(0, RoundingMode.DOWN).toLong(),
            voterRewards = 0, // AMM은 voter rewards 없음
            message = "AMM settlement finalized."
        )
    }

    /**
     * 오더북 정산 로직 (기존 로직)
     * 기존 finalizeSettlement() 로직을 그대로 이동
     */
    @Transactional
    private fun finalizeOrderbookSettlement(question: Question, skipDeadlineCheck: Boolean): SettlementResult {
        val questionId = question.id ?: throw IllegalStateException("Question ID is null")

        val finalResult = question.finalResult
        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("Cannot finalize settlement without confirmed final result.")
        }
        val policy = settlementPolicyFactory.getPolicy(question.type)
        val winningChoice = policy.determineWinningChoice(finalResult)

        // 상태 확정 + disputeDeadline null로 설정 (이중 정산 방지 마커)
        question.status = QuestionStatus.SETTLED
        question.disputeDeadline = null
        questionRepository.save(question)

        // === 1회 조회: 투표 활동 조회 (티어/보상용) ===
        val allActivities = activityRepository.findByQuestionId(questionId)
        val votes = allActivities.filter { it.activityType == com.predata.backend.domain.ActivityType.VOTE }

        // === 포지션 기반 정산 (BET 대체) ===
        val positions = positionService.getPositionsByQuestionId(questionId)

        // winning side 결정
        val winningSide = if (winningChoice == Choice.YES) OrderSide.YES else OrderSide.NO

        // === 1회 조회: 관련 멤버 전체 배치 조회 ===
        val allMemberIds = (votes.map { it.memberId } + positions.map { it.memberId }).distinct()
        val membersMap = if (allMemberIds.isNotEmpty()) {
            memberRepository.findAllByIdIn(allMemberIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        var totalWinners = 0
        var totalPayout = BigDecimal.ZERO
        var totalRewardPool = 0L
        var totalFeeCollected = BigDecimal.ZERO

        // === 메모리 처리 1: 포지션 기반 승자 배당금 지급 (0.99 multiplier) ===
        val winningPositions = positions.filter { it.side == winningSide }
        winningPositions.forEach { position ->
            val member = membersMap[position.memberId]
            if (member != null) {
                // 0.99 multiplier 적용 (1% 수수료) - BigDecimal로 정밀 처리
                val payout = position.quantity.multiply(BigDecimal("0.99")).setScale(6, RoundingMode.DOWN)
                val fee = position.quantity.multiply(BigDecimal("0.01")).setScale(6, RoundingMode.DOWN)

                member.usdcBalance = member.usdcBalance.add(payout)
                transactionHistoryService.record(
                    memberId = position.memberId,
                    type = "SETTLEMENT",
                    amount = payout,
                    balanceAfter = member.usdcBalance,
                    description = "포지션 정산 승리 - Question #$questionId",
                    questionId = questionId
                )
                totalWinners++
                totalPayout = totalPayout.add(payout)
                totalFeeCollected = totalFeeCollected.add(fee)
            }
        }

        // 수집된 수수료를 FeePool에 기록
        if (totalFeeCollected > BigDecimal.ZERO) {
            feePoolService.collectFee(questionId, totalFeeCollected)
        }

        // 정산 완료 시 모든 포지션에 settled=true 마킹
        positionService.markAsSettled(questionId)

        // === 메모리 처리 2: 티어 업데이트 (TierService 로직 인라인) ===
        votes.forEach { vote ->
            val member = membersMap[vote.memberId] ?: return@forEach
            val isCorrect = vote.choice == winningChoice

            if (isCorrect) {
                member.accuracyScore += TierService.CORRECT_PREDICTION_POINTS
                member.correctPredictions++
            } else {
                member.accuracyScore += TierService.WRONG_PREDICTION_PENALTY
            }
            member.totalPredictions++

            if (member.accuracyScore < 0) {
                member.accuracyScore = 0
            }

            val newTier = tierService.calculateTier(member.accuracyScore)
            if (newTier != member.tier) {
                member.tier = newTier
                member.tierWeight = TierService.TIER_WEIGHTS[newTier] ?: BigDecimal("1.00")
            }
        }

        // === 메모리 처리 3: 보상 분배 (새 엔진 사용) ===
        if (votes.isNotEmpty()) {
            // 수수료는 이미 개별 payout에서 수집됨 (0.99 multiplier)
            // 보상 풀 계산: 수집된 수수료의 50%
            val rewardPool = totalFeeCollected.multiply(BigDecimal.valueOf(RewardService.REWARD_POOL_PERCENTAGE)).toLong()
            totalRewardPool = rewardPool

            logger.info("[Settlement] Fee collected from payouts: questionId={}, totalFee={}, rewardPool={}",
                questionId, totalFeeCollected, rewardPool)

            // 새로운 보상 분배 엔진 사용 (VoteRewardDistributionService)
            // - 레벨 기반 가중치, idempotency 보장, 재시도 메커니즘
            // - 분배 실패해도 정산은 롤백하지 않음 (수동 재시도 가능)
            try {
                val distributionResult = voteRewardDistributionService.distributeRewards(questionId)
                logger.info("[Settlement] Reward distribution completed: questionId={}, result={}", questionId, distributionResult)
            } catch (e: Exception) {
                logger.error("[Settlement] Reward distribution failed (manual retry required) questionId={}: {}", questionId, e.message, e)
                // Settlement itself succeeds even if distribution fails (admin can retry via /api/admin/rewards/retry)
            }

            // @Deprecated: 레거시 티어 가중치 기반 보상 분배 로직 제거됨
            // - 새로운 VoteRewardDistributionService가 레벨 기반 가중치로 처리
            // - 기존 로직은 usdcBalance에 직접 추가했지만, 새 엔진은 pointBalance에 적립
        }

        // managed 엔티티 → @Transactional 커밋 시 자동 dirty-check flush
        // saveAll() 불필요 (merge() N번 오버헤드 제거)

        // 온체인 기록
        blockchainService.settleQuestionOnChain(questionId, finalResult)

        // === Badge 업데이트: 정산 후 뱃지 체크 (포지션 기반) ===
        positions.forEach { position ->
            try {
                val member = membersMap[position.memberId] ?: return@forEach
                val isWinner = position.side == winningSide
                val payoutRatio = if (isWinner) {
                    // 폴리마켓 방식: 1.0 배율 (quantity * 1.0)
                    1.0
                } else {
                    0.0
                }
                badgeService.onSettlement(position.memberId, isWinner, payoutRatio)
                badgeService.onPointsChange(position.memberId, member.usdcBalance.toLong())
            } catch (e: Exception) {
                logger.warn("[Settlement] Badge update failed member={}: {}", position.memberId, e.message)
            }
        }

        // 티어 변경 뱃지 체크
        votes.forEach { vote ->
            try {
                val member = membersMap[vote.memberId] ?: return@forEach
                badgeService.onTierChange(vote.memberId, member.tier)
            } catch (e: Exception) {
                logger.warn("[Settlement] Tier Badge update failed member={}: {}", vote.memberId, e.message)
            }
        }

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = positions.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout.setScale(0, RoundingMode.DOWN).toLong(),
            voterRewards = totalRewardPool,
            message = "Settlement finalized. (rewards: ${totalRewardPool}P distributed)"
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
     * 사용자별 정산 내역 조회 (포지션 기반)
     */
    fun getSettlementHistory(memberId: Long): List<SettlementHistoryItem> {
        val positions = positionService.getPositions(memberId)
            .filter { it.settled }

        return positions.mapNotNull { position ->
            val question = questionRepository.findById(position.questionId).orElse(null)
            if (question != null && question.status == QuestionStatus.SETTLED) {
                val policy = settlementPolicyFactory.getPolicy(question.type)
                val finalResultChoice = policy.determineWinningChoice(question.finalResult)
                val winningSide = if (finalResultChoice == Choice.YES) OrderSide.YES else OrderSide.NO
                val isWinner = position.side == winningSide
                val payout = if (isWinner) {
                    position.quantity.toLong()  // 폴리마켓: quantity * 1.0
                } else {
                    0L
                }
                val betAmount = position.quantity.multiply(position.avgPrice).toLong()

                SettlementHistoryItem(
                    questionId = question.id ?: 0,
                    questionTitle = question.title,
                    myChoice = if (position.side == OrderSide.YES) "YES" else "NO",
                    finalResult = question.finalResult.name,
                    betAmount = betAmount,
                    payout = payout,
                    profit = payout - betAmount,
                    isWinner = isWinner
                )
            } else {
                null
            }
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
