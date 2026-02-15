package com.predata.backend.service

import com.predata.backend.domain.Choice
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.OrderSide
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.service.settlement.SettlementPolicyFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
    private val auditService: AuditService
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
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        // 1. marketType == VERIFIABLE 체크
        if (question.marketType != com.predata.backend.domain.MarketType.VERIFIABLE) {
            logger.info("[AutoSettle] 질문 #{} VERIFIABLE이 아님, 수동 정산 대기", questionId)
            return null
        }

        // ResolutionAdapter를 통해 결과 조회
        val resolutionResult = try {
            resolutionAdapterRegistry.resolve(question)
        } catch (e: Exception) {
            logger.error("[AutoSettle] 질문 #{} 결과 조회 실패: {}", questionId, e.message)
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
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("이미 정산된 질문입니다.")
        }
        if (question.status != QuestionStatus.VOTING && question.status != QuestionStatus.BETTING && question.status != QuestionStatus.BREAK) {
            throw IllegalStateException("정산 가능한 상태가 아닙니다. (현재: ${question.status})")
        }

        // ResolutionAdapter를 통해 자동 정산
        val resolutionResult = resolutionAdapterRegistry.resolve(question)

        // 결과가 미확정이면 정산 시작 불가
        val finalResult = resolutionResult.result
            ?: throw IllegalStateException("정산 결과가 아직 확정되지 않았습니다.")
        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("정산 결과가 아직 확정되지 않았습니다.")
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
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("이미 정산된 질문입니다.")
        }
        if (question.status != QuestionStatus.VOTING && question.status != QuestionStatus.BETTING && question.status != QuestionStatus.BREAK) {
            throw IllegalStateException("정산 가능한 상태가 아닙니다. (현재: ${question.status})")
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

        // Position 기준으로 정산 대상 확인
        val positions = positionService.getPositionsByQuestionId(questionId)

        if (positions.isEmpty()) {
            throw IllegalStateException("정산 대상 포지션이 없습니다.")
        }

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = positions.size,
            totalWinners = 0,
            totalPayout = 0,
            voterRewards = 0,
            message = "정산이 시작되었습니다. ${DISPUTE_HOURS}시간 이의 제기 기간 후 확정됩니다."
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
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        if (question.status != QuestionStatus.SETTLED) {
            throw IllegalStateException("정산 대기 상태의 질문만 확정할 수 있습니다. (현재: ${question.status})")
        }

        // 이중 정산 방지: disputeDeadline이 null이면 이미 finalize 완료된 질문
        if (question.disputeDeadline == null) {
            throw IllegalStateException("이미 정산 확정된 질문입니다.")
        }

        if (!skipDeadlineCheck && LocalDateTime.now().isBefore(question.disputeDeadline)) {
            throw IllegalStateException("이의 제기 기간이 아직 종료되지 않았습니다. (기한: ${question.disputeDeadline})")
        }

        val finalResult = question.finalResult
        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("최종 결과가 확정되지 않아 정산을 확정할 수 없습니다.")
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
        var totalPayout = 0L
        var totalRewardPool = 0L

        // === 메모리 처리 1: 포지션 기반 승자 배당금 지급 ===
        val winningPositions = positions.filter { it.side == winningSide }
        winningPositions.forEach { position ->
            val member = membersMap[position.memberId]
            if (member != null) {
                // 폴리마켓 방식: winning side는 quantity * 1.0 포인트 지급
                val payout = position.quantity.toLong()
                member.usdcBalance = member.usdcBalance.add(BigDecimal(payout))
                transactionHistoryService.record(
                    memberId = position.memberId,
                    type = "SETTLEMENT",
                    amount = BigDecimal(payout),
                    balanceAfter = member.usdcBalance,
                    description = "포지션 정산 승리 - Question #$questionId",
                    questionId = questionId
                )
                totalWinners++
                totalPayout += payout
            }
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

        // === 메모리 처리 3: 보상 분배 (RewardService 로직 인라인) ===
        if (votes.isNotEmpty()) {
            val totalBetAmount = question.totalBetPool
            val totalFee = (totalBetAmount * RewardService.FEE_PERCENTAGE).toLong()
            val rewardPool = (totalFee * RewardService.REWARD_POOL_PERCENTAGE).toLong()
            totalRewardPool = rewardPool

            // 티어 가중치 합계 계산
            var totalWeight = BigDecimal.ZERO
            val voterWeights = mutableMapOf<Long, BigDecimal>()
            votes.forEach { vote ->
                val member = membersMap[vote.memberId]
                if (member != null) {
                    voterWeights[vote.memberId] = member.tierWeight
                    totalWeight = totalWeight.add(member.tierWeight)
                }
            }

            // 가중치에 따라 보상 분배
            if (totalWeight > BigDecimal.ZERO) {
                voterWeights.forEach { (memberId, weight) ->
                    val member = membersMap[memberId]
                    if (member != null) {
                        val rewardAmount = BigDecimal(rewardPool)
                            .multiply(weight)
                            .divide(totalWeight, 0, java.math.RoundingMode.DOWN)
                            .toLong()
                        member.usdcBalance = member.usdcBalance.add(BigDecimal(rewardAmount))
                        if (rewardAmount > 0) {
                            transactionHistoryService.record(
                                memberId = memberId,
                                type = "SETTLEMENT",
                                amount = BigDecimal(rewardAmount),
                                balanceAfter = member.usdcBalance,
                                description = "투표 보상 - Question #$questionId",
                                questionId = questionId
                            )
                        }
                    }
                }
            }
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
                logger.warn("[Settlement] Badge 업데이트 실패 member={}: {}", position.memberId, e.message)
            }
        }

        // 티어 변경 뱃지 체크
        votes.forEach { vote ->
            try {
                val member = membersMap[vote.memberId] ?: return@forEach
                badgeService.onTierChange(vote.memberId, member.tier)
            } catch (e: Exception) {
                logger.warn("[Settlement] Tier Badge 업데이트 실패 member={}: {}", vote.memberId, e.message)
            }
        }

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = positions.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout,
            voterRewards = totalRewardPool,
            message = "정산이 확정되었습니다. (보상: ${totalRewardPool}P 분배)"
        )
    }

    /**
     * 정산 취소 (PENDING_SETTLEMENT → OPEN)
     * 이의 제기로 인해 정산을 취소하고 질문을 다시 열린 상태로 되돌린다.
     */
    @Transactional
    fun cancelPendingSettlement(questionId: Long): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        if (question.status != QuestionStatus.SETTLED) {
            throw IllegalStateException("정산 대기 상태의 질문만 취소할 수 있습니다. (현재: ${question.status})")
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
                "정산이 취소되었습니다. 질문이 만료되어 SETTLED 상태로 전환되었습니다."
            else
                "정산이 취소되었습니다. 질문이 다시 BETTING 상태로 전환되었습니다."
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
