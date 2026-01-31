package com.predata.backend.service

import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class SettlementService(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val tierService: TierService,
    private val rewardService: RewardService,
    private val blockchainService: BlockchainService
) {

    /**
     * 질문 결과 확정 및 정산
     * @param questionId 질문 ID
     * @param finalResult 최종 결과 (YES or NO)
     */
    @Transactional
    fun settleQuestion(questionId: Long, finalResult: FinalResult): SettlementResult {
        // 1. 질문 조회
        val question = questionRepository.findById(questionId)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다.") }

        // 2. 이미 정산된 질문인지 확인
        if (question.status == "SETTLED") {
            throw IllegalStateException("이미 정산된 질문입니다.")
        }

        // 3. 질문 상태 업데이트
        question.status = "SETTLED"
        question.finalResult = finalResult
        questionRepository.save(question)

        // 4. 베팅 내역 조회 (투표는 제외)
        val bets = activityRepository.findByQuestionIdAndActivityType(
            questionId, 
            com.predata.backend.domain.ActivityType.BET
        )

        val winningChoice = if (finalResult == FinalResult.YES) Choice.YES else Choice.NO
        var totalWinners = 0
        var totalPayout = 0L

        // 5. 승자에게 배당금 지급
        bets.filter { it.choice == winningChoice }.forEach { bet ->
            val member = memberRepository.findById(bet.memberId).orElse(null)
            if (member != null) {
                // 배당금 계산
                val payout = calculatePayout(
                    betAmount = bet.amount,
                    totalPool = question.totalBetPool,
                    winningPool = if (finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
                )

                // 포인트 지급
                member.pointBalance += payout
                memberRepository.save(member)

                totalWinners++
                totalPayout += payout
            }
        }

        // 6. 티어 자동 업데이트 (정확도 기반)
        tierService.updateTiersAfterSettlement(questionId, finalResult)

        // 7. 티케터 보상 분배 (베팅 수수료 기반)
        val rewardResult = rewardService.distributeRewards(questionId)

        // 8. 온체인에 정산 결과 기록 (비동기)
        blockchainService.settleQuestionOnChain(questionId, finalResult)

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = bets.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout,
            voterRewards = rewardResult.totalRewardPool,
            message = "정산이 완료되었습니다. (보상: ${rewardResult.totalRewardPool}P 분배)"
        )
    }

    /**
     * 배당금 계산
     * AMM 방식: (베팅 금액 / 승리 풀) * 전체 풀 * 0.99 (수수료 1%)
     */
    private fun calculatePayout(
        betAmount: Long,
        totalPool: Long,
        winningPool: Long
    ): Long {
        if (winningPool == 0L) return betAmount // 패자가 없으면 원금 반환

        val payoutRatio = BigDecimal(totalPool)
            .divide(BigDecimal(winningPool), 10, RoundingMode.HALF_UP)
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
            if (question != null && question.status == "SETTLED") {
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
