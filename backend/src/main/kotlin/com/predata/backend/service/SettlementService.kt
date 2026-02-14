package com.predata.backend.service

import com.predata.backend.domain.Choice
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.FinalResult
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.MemberRepository
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
    private val transactionHistoryService: TransactionHistoryService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SettlementService::class.java)

    companion object {
        private const val DISPUTE_HOURS = 0L // 테스트용: 이의제기 기간 없음 (즉시 확정)
        // TODO: 실제 운영에서는 24L로 변경
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

        val bets = activityRepository.findByQuestionIdAndActivityType(
            questionId, com.predata.backend.domain.ActivityType.BET
        )

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = bets.size,
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
        val winningChoice = if (finalResult == FinalResult.YES) Choice.YES else Choice.NO

        // 상태 확정 + disputeDeadline null로 설정 (이중 정산 방지 마커)
        question.status = QuestionStatus.SETTLED
        question.disputeDeadline = null
        questionRepository.save(question)

        // === 1회 조회: 모든 활동(투표+베팅) 조회 ===
        val allActivities = activityRepository.findByQuestionId(questionId)
        val votes = allActivities.filter { it.activityType == com.predata.backend.domain.ActivityType.VOTE }
        val bets = allActivities.filter { it.activityType == com.predata.backend.domain.ActivityType.BET }

        // === 1회 조회: 관련 멤버 전체 배치 조회 ===
        val allMemberIds = (votes.map { it.memberId } + bets.map { it.memberId }).distinct()
        val membersMap = if (allMemberIds.isNotEmpty()) {
            memberRepository.findAllByIdIn(allMemberIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        var totalWinners = 0
        var totalPayout = 0L
        var totalRewardPool = 0L

        // === 메모리 처리 1: 베팅 승자 배당금 지급 ===
        val winningBets = bets.filter { it.choice == winningChoice }
        winningBets.forEach { bet ->
            val member = membersMap[bet.memberId]
            if (member != null) {
                val payout = calculatePayout(
                    betAmount = bet.amount,
                    totalPool = question.totalBetPool,
                    winningPool = if (finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
                )
                member.usdcBalance = member.usdcBalance.add(BigDecimal(payout))
                transactionHistoryService.record(
                    memberId = bet.memberId,
                    type = "SETTLEMENT",
                    amount = BigDecimal(payout),
                    balanceAfter = member.usdcBalance,
                    description = "베팅 정산 승리 - Question #$questionId",
                    questionId = questionId
                )
                totalWinners++
                totalPayout += payout
            }
        }

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

        // === Badge 업데이트: 정산 후 뱃지 체크 ===
        bets.forEach { bet ->
            try {
                val member = membersMap[bet.memberId] ?: return@forEach
                val isWinner = bet.choice == winningChoice
                val payoutRatio = if (isWinner && bet.amount > 0) {
                    calculatePayout(bet.amount, question.totalBetPool,
                        if (finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
                    ).toDouble() / bet.amount.toDouble()
                } else 0.0
                badgeService.onSettlement(bet.memberId, isWinner, payoutRatio)
                badgeService.onPointsChange(bet.memberId, member.usdcBalance.toLong())
            } catch (e: Exception) {
                logger.warn("[Settlement] Badge 업데이트 실패 member={}: {}", bet.memberId, e.message)
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
            totalBets = bets.size,
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
     * 배당금 계산
     * AMM 방식: (베팅 금액 / 실질 승리풀) * 실질 전체풀 * 0.99 (수수료 1%)
     * 초기 유동성(하우스 머니)을 제외한 실제 베팅 금액만으로 계산
     */
    private fun calculatePayout(
        betAmount: Long,
        totalPool: Long,
        winningPool: Long
    ): Long {
        val initialLiquidity = QuestionManagementService.INITIAL_LIQUIDITY
        val effectiveTotalPool = maxOf(0L, totalPool - initialLiquidity * 2)
        val effectiveWinningPool = maxOf(0L, winningPool - initialLiquidity)

        if (effectiveWinningPool == 0L) return betAmount // 실질 승리풀이 없으면 원금 반환

        val payoutRatio = BigDecimal(effectiveTotalPool)
            .divide(BigDecimal(effectiveWinningPool), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal("0.99")) // 수수료 1%

        return BigDecimal(betAmount)
            .multiply(payoutRatio)
            .setScale(0, RoundingMode.DOWN)
            .toLong()
    }

    /**
     * 사용자별 정산 내역 조회
     */
    fun getSettlementHistory(memberId: Long): List<SettlementHistoryItem> {
        val allBets = activityRepository.findByMemberIdAndActivityType(
            memberId,
            com.predata.backend.domain.ActivityType.BET
        )

        return allBets.mapNotNull { bet ->
            val question = questionRepository.findById(bet.questionId).orElse(null)
            if (question != null && question.status == QuestionStatus.SETTLED) {
                val finalResultChoice = if (question.finalResult == FinalResult.YES) Choice.YES else Choice.NO
                val isWinner = bet.choice == finalResultChoice
                val payout = if (isWinner) {
                    calculatePayout(
                        betAmount = bet.amount,
                        totalPool = question.totalBetPool,
                        winningPool = if (bet.choice == Choice.YES) question.yesBetPool else question.noBetPool
                    )
                } else {
                    0L
                }

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
