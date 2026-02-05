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
    private val blockchainService: BlockchainService
) {

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
        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("이미 정산 대기 중인 질문입니다.")
        }
        if (question.status != QuestionStatus.VOTING && question.status != QuestionStatus.BETTING) {
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
     */
    @Transactional
    fun finalizeSettlement(questionId: Long, skipDeadlineCheck: Boolean = false): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        if (question.status != QuestionStatus.SETTLED) {
            throw IllegalStateException("정산 대기 상태의 질문만 확정할 수 있습니다. (현재: ${question.status})")
        }

        if (!skipDeadlineCheck && question.disputeDeadline != null && LocalDateTime.now().isBefore(question.disputeDeadline)) {
            throw IllegalStateException("이의 제기 기간이 아직 종료되지 않았습니다. (기한: ${question.disputeDeadline})")
        }

        val finalResult = question.finalResult

        // 상태 확정
        question.status = QuestionStatus.SETTLED
        questionRepository.save(question)

        // 베팅 내역 조회
        val bets = activityRepository.findByQuestionIdAndActivityType(
            questionId, com.predata.backend.domain.ActivityType.BET
        )

        val winningChoice = if (finalResult == FinalResult.YES) Choice.YES else Choice.NO
        var totalWinners = 0
        var totalPayout = 0L

        // 승자에게 배당금 지급
        bets.filter { it.choice == winningChoice }.forEach { bet ->
            val member = memberRepository.findById(bet.memberId).orElse(null)
            if (member != null) {
                val payout = calculatePayout(
                    betAmount = bet.amount,
                    totalPool = question.totalBetPool,
                    winningPool = if (finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
                )
                member.pointBalance += payout
                memberRepository.save(member)
                totalWinners++
                totalPayout += payout
            }
        }

        // 티어 자동 업데이트
        tierService.updateTiersAfterSettlement(questionId, finalResult)

        // 보상 분배
        val rewardResult = rewardService.distributeRewards(questionId)

        // 온체인 기록
        blockchainService.settleQuestionOnChain(questionId, finalResult)

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = bets.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout,
            voterRewards = rewardResult.totalRewardPool,
            message = "정산이 확정되었습니다. (보상: ${rewardResult.totalRewardPool}P 분배)"
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
