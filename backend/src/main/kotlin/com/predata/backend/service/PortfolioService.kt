package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

@Service
class PortfolioService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository
) {

    companion object {
        private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
    }

    /**
     * Portfolio summary: aggregate stats across all bets for a member.
     */
    @Transactional(readOnly = true)
    fun getPortfolioSummary(memberId: Long): PortfolioSummaryResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("회원을 찾을 수 없습니다.") }

        val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)

        val totalInvested = bets.sumOf { it.amount }

        var totalReturns = 0L
        var unrealizedValue = 0L
        var wins = 0
        var losses = 0
        var openBets = 0
        var settledBets = 0

        for (bet in bets) {
            val question = questionRepository.findById(bet.questionId).orElse(null) ?: continue

            when (question.status) {
                QuestionStatus.SETTLED -> {
                    settledBets++
                    val winningChoice = if (question.finalResult == FinalResult.YES) Choice.YES else Choice.NO
                    if (bet.choice == winningChoice) {
                        val payout = calculatePayout(
                            betAmount = bet.amount,
                            totalPool = question.totalBetPool,
                            winningPool = if (question.finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
                        )
                        totalReturns += payout
                        wins++
                    } else {
                        losses++
                    }
                }
                QuestionStatus.VOTING, QuestionStatus.BREAK, QuestionStatus.BETTING -> {
                    openBets++
                    val estimatedPayout = estimateOpenPayout(
                        betAmount = bet.amount,
                        choice = bet.choice,
                        totalPool = question.totalBetPool,
                        yesBetPool = question.yesBetPool,
                        noBetPool = question.noBetPool
                    )
                    unrealizedValue += estimatedPayout
                }
            }
        }

        val netProfit = totalReturns - totalInvested + unrealizedValue
        val totalBets = bets.size
        val winRate = if (settledBets > 0) wins.toDouble() / settledBets * 100.0 else 0.0
        val roi = if (totalInvested > 0) (totalReturns - totalInvested).toDouble() / totalInvested * 100.0 else 0.0

        return PortfolioSummaryResponse(
            memberId = memberId,
            totalInvested = totalInvested,
            totalReturns = totalReturns,
            netProfit = netProfit,
            unrealizedValue = unrealizedValue,
            currentBalance = member.pointBalance,
            winRate = BigDecimal(winRate).setScale(1, RoundingMode.HALF_UP).toDouble(),
            totalBets = totalBets,
            openBets = openBets,
            settledBets = settledBets,
            roi = BigDecimal(roi).setScale(2, RoundingMode.HALF_UP).toDouble()
        )
    }

    /**
     * Open positions: bets on questions that have not yet settled.
     */
    @Transactional(readOnly = true)
    fun getOpenPositions(memberId: Long): List<OpenPositionResponse> {
        val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)

        return bets.mapNotNull { bet ->
            val question = questionRepository.findById(bet.questionId).orElse(null) ?: return@mapNotNull null
            if (question.status !in listOf(QuestionStatus.VOTING, QuestionStatus.BREAK, QuestionStatus.BETTING)) return@mapNotNull null

            val totalPool = question.totalBetPool.toDouble()
            val yesPercentage = if (totalPool > 0) question.yesBetPool.toDouble() / totalPool * 100.0 else 50.0
            val noPercentage = if (totalPool > 0) question.noBetPool.toDouble() / totalPool * 100.0 else 50.0

            val estimatedPayout = estimateOpenPayout(
                betAmount = bet.amount,
                choice = bet.choice,
                totalPool = question.totalBetPool,
                yesBetPool = question.yesBetPool,
                noBetPool = question.noBetPool
            )

            OpenPositionResponse(
                activityId = bet.id ?: 0,
                questionId = question.id ?: 0,
                questionTitle = question.title,
                category = question.category,
                choice = bet.choice.name,
                betAmount = bet.amount,
                currentYesPercentage = BigDecimal(yesPercentage).setScale(1, RoundingMode.HALF_UP).toDouble(),
                currentNoPercentage = BigDecimal(noPercentage).setScale(1, RoundingMode.HALF_UP).toDouble(),
                estimatedPayout = estimatedPayout,
                estimatedProfitLoss = estimatedPayout - bet.amount,
                expiresAt = question.expiredAt.toString(),
                placedAt = bet.createdAt.toString()
            )
        }
    }

    /**
     * Category breakdown: group bets by question category and aggregate performance.
     */
    @Transactional(readOnly = true)
    fun getCategoryBreakdown(memberId: Long): List<CategoryPerformanceResponse> {
        val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)

        data class CategoryAccum(
            var totalBets: Int = 0,
            var wins: Int = 0,
            var losses: Int = 0,
            var pending: Int = 0,
            var invested: Long = 0,
            var returned: Long = 0
        )

        val categoryMap = mutableMapOf<String, CategoryAccum>()

        for (bet in bets) {
            val question = questionRepository.findById(bet.questionId).orElse(null) ?: continue
            val category = question.category ?: "OTHER"
            val accum = categoryMap.getOrPut(category) { CategoryAccum() }

            accum.totalBets++
            accum.invested += bet.amount

            when (question.status) {
                QuestionStatus.SETTLED -> {
                    val winningChoice = if (question.finalResult == FinalResult.YES) Choice.YES else Choice.NO
                    if (bet.choice == winningChoice) {
                        val payout = calculatePayout(
                            betAmount = bet.amount,
                            totalPool = question.totalBetPool,
                            winningPool = if (question.finalResult == FinalResult.YES) question.yesBetPool else question.noBetPool
                        )
                        accum.returned += payout
                        accum.wins++
                    } else {
                        accum.losses++
                    }
                }
                else -> {
                    accum.pending++
                }
            }
        }

        return categoryMap.map { (category, accum) ->
            val settled = accum.wins + accum.losses
            val winRate = if (settled > 0) accum.wins.toDouble() / settled * 100.0 else 0.0
            CategoryPerformanceResponse(
                category = category,
                totalBets = accum.totalBets,
                wins = accum.wins,
                losses = accum.losses,
                pending = accum.pending,
                invested = accum.invested,
                returned = accum.returned,
                profit = accum.returned - accum.invested,
                winRate = BigDecimal(winRate).setScale(1, RoundingMode.HALF_UP).toDouble()
            )
        }.sortedByDescending { it.totalBets }
    }

    /**
     * Accuracy trend: monthly accuracy for settled bets.
     */
    @Transactional(readOnly = true)
    fun getAccuracyTrend(memberId: Long): List<AccuracyTrendPointResponse> {
        val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)

        data class MonthAccum(
            var total: Int = 0,
            var correct: Int = 0
        )

        val monthMap = sortedMapOf<String, MonthAccum>()

        for (bet in bets) {
            val question = questionRepository.findById(bet.questionId).orElse(null) ?: continue
            if (question.status != QuestionStatus.SETTLED) continue

            val monthKey = question.createdAt.format(MONTH_FORMATTER)
            val accum = monthMap.getOrPut(monthKey) { MonthAccum() }
            accum.total++

            val winningChoice = if (question.finalResult == FinalResult.YES) Choice.YES else Choice.NO
            if (bet.choice == winningChoice) {
                accum.correct++
            }
        }

        var cumulativeTotal = 0
        var cumulativeCorrect = 0

        return monthMap.map { (month, accum) ->
            cumulativeTotal += accum.total
            cumulativeCorrect += accum.correct

            val accuracy = if (accum.total > 0) accum.correct.toDouble() / accum.total * 100.0 else 0.0
            val cumulativeAccuracy = if (cumulativeTotal > 0) cumulativeCorrect.toDouble() / cumulativeTotal * 100.0 else 0.0

            AccuracyTrendPointResponse(
                date = month,
                totalPredictions = accum.total,
                correctPredictions = accum.correct,
                accuracy = BigDecimal(accuracy).setScale(1, RoundingMode.HALF_UP).toDouble(),
                cumulativeAccuracy = BigDecimal(cumulativeAccuracy).setScale(1, RoundingMode.HALF_UP).toDouble()
            )
        }
    }

    /**
     * Payout calculation matching SettlementService AMM formula.
     * Uses the same INITIAL_LIQUIDITY from QuestionManagementService.
     */
    private fun calculatePayout(betAmount: Long, totalPool: Long, winningPool: Long): Long {
        val initialLiquidity = QuestionManagementService.INITIAL_LIQUIDITY
        val effectiveTotalPool = maxOf(0L, totalPool - initialLiquidity * 2)
        val effectiveWinningPool = maxOf(0L, winningPool - initialLiquidity)

        if (effectiveWinningPool == 0L) return betAmount

        val payoutRatio = BigDecimal(effectiveTotalPool)
            .divide(BigDecimal(effectiveWinningPool), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal("0.99"))

        return BigDecimal(betAmount)
            .multiply(payoutRatio)
            .setScale(0, RoundingMode.DOWN)
            .toLong()
    }

    /**
     * Estimate payout for open positions assuming the user's chosen side wins.
     */
    private fun estimateOpenPayout(
        betAmount: Long,
        choice: Choice,
        totalPool: Long,
        yesBetPool: Long,
        noBetPool: Long
    ): Long {
        val winningPool = if (choice == Choice.YES) yesBetPool else noBetPool
        return calculatePayout(betAmount, totalPool, winningPool)
    }
}

// ===== DTOs =====

data class PortfolioSummaryResponse(
    val memberId: Long,
    val totalInvested: Long,
    val totalReturns: Long,
    val netProfit: Long,
    val unrealizedValue: Long,
    val currentBalance: Long,
    val winRate: Double,
    val totalBets: Int,
    val openBets: Int,
    val settledBets: Int,
    val roi: Double
)

data class OpenPositionResponse(
    val activityId: Long,
    val questionId: Long,
    val questionTitle: String,
    val category: String?,
    val choice: String,
    val betAmount: Long,
    val currentYesPercentage: Double,
    val currentNoPercentage: Double,
    val estimatedPayout: Long,
    val estimatedProfitLoss: Long,
    val expiresAt: String,
    val placedAt: String
)

data class CategoryPerformanceResponse(
    val category: String,
    val totalBets: Int,
    val wins: Int,
    val losses: Int,
    val pending: Int,
    val invested: Long,
    val returned: Long,
    val profit: Long,
    val winRate: Double
)

data class AccuracyTrendPointResponse(
    val date: String,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val accuracy: Double,
    val cumulativeAccuracy: Double
)
