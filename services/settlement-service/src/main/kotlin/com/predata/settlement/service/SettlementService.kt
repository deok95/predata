package com.predata.settlement.service

import com.predata.common.client.BettingClient
import com.predata.common.client.MemberClient
import com.predata.common.client.QuestionClient
import com.predata.common.config.EventPublisher
import com.predata.common.domain.FinalResult
import com.predata.common.dto.FinalResultRequest
import com.predata.common.dto.PointsRequest
import com.predata.common.events.QuestionSettledEvent
import com.predata.common.events.SettlementCompletedEvent
import com.predata.settlement.domain.Settlement
import com.predata.settlement.repository.SettlementRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class SettlementService(
    private val questionClient: QuestionClient,
    private val bettingClient: BettingClient,
    private val memberClient: MemberClient,
    private val rewardService: RewardService,
    private val settlementRepository: SettlementRepository,
    private val eventPublisher: EventPublisher
) {
    private val log = LoggerFactory.getLogger(SettlementService::class.java)

    /**
     * 질문 결과 확정 및 정산
     * @param questionId 질문 ID
     * @param finalResult 최종 결과 (YES or NO)
     */
    @Transactional
    fun settleQuestion(questionId: Long, finalResult: FinalResult): SettlementResult {
        // 1. 이미 정산된 질문인지 확인
        if (settlementRepository.existsByQuestionId(questionId)) {
            throw IllegalStateException("이미 정산된 질문입니다.")
        }

        // 2. 질문 조회
        val questionResponse = questionClient.getQuestion(questionId)
        val question = questionResponse.data ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        // 3. 질문 상태 업데이트 (Question Service에 요청)
        questionClient.updateFinalResult(
            questionId,
            FinalResultRequest(finalResult = finalResult.name, status = "SETTLED")
        )

        // 4. 베팅 내역 조회 (Betting Service에 요청)
        val betsResponse = bettingClient.getActivitiesByQuestion(questionId, "BET")
        val bets = betsResponse.data ?: emptyList()

        val winningChoice = if (finalResult == FinalResult.YES) "YES" else "NO"
        var totalWinners = 0
        var totalPayout = 0L

        // 5. 승자에게 배당금 지급
        bets.filter { it.choice == winningChoice }.forEach { bet ->
            // 배당금 계산
            val payout = calculatePayout(
                betAmount = bet.amount,
                totalPool = question.totalBetPool,
                winningPool = if (finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
            )

            // 포인트 지급 (Member Service에 요청)
            memberClient.addPoints(bet.memberId, PointsRequest(payout))

            totalWinners++
            totalPayout += payout
        }

        // 6. 정산 내역 저장
        val settlement = Settlement(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = bets.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout
        )
        settlementRepository.save(settlement)

        // 7. 티케터 보상 분배 (베팅 수수료 기반)
        val rewardResult = rewardService.distributeRewards(questionId)

        // 8. 이벤트 발행 (Redis 미연결 시 무시)
        try {
            eventPublisher.publishSettlementCompletedEvent(
                SettlementCompletedEvent(
                    questionId = questionId,
                    finalResult = finalResult.name,
                    totalWinners = totalWinners,
                    totalPayout = totalPayout
                )
            )
            eventPublisher.publishQuestionSettledEvent(
                QuestionSettledEvent(
                    questionId = questionId,
                    finalResult = finalResult.name
                )
            )
        } catch (e: Exception) {
            log.warn("Redis 이벤트 발행 실패 (무시): ${e.message}")
        }

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
    @Transactional(readOnly = true)
    fun getSettlementHistory(memberId: Long): List<SettlementHistoryItem> {
        // Betting Service에서 사용자의 베팅 내역 조회
        val betsResponse = bettingClient.getActivitiesByMember(memberId)
        val allBets = betsResponse.data?.filter { it.activityType == "BET" } ?: emptyList()

        return allBets.mapNotNull { bet ->
            // Settlement 조회
            val settlement = settlementRepository.findByQuestionId(bet.questionId)
            if (settlement != null) {
                // Question 조회
                val questionResponse = questionClient.getQuestion(bet.questionId)
                val question = questionResponse.data

                if (question != null) {
                    val isWinner = bet.choice == settlement.finalResult
                    val payout = if (isWinner) {
                        calculatePayout(
                            betAmount = bet.amount,
                            totalPool = question.totalBetPool,
                            winningPool = if (bet.choice == "YES") question.yesBetPool else question.noBetPool
                        )
                    } else {
                        0L
                    }

                    SettlementHistoryItem(
                        questionId = question.id,
                        questionTitle = question.title,
                        myChoice = bet.choice,
                        finalResult = settlement.finalResult,
                        betAmount = bet.amount,
                        payout = payout,
                        profit = payout - bet.amount,
                        isWinner = isWinner
                    )
                } else {
                    null
                }
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
