package com.predata.backend.service.settlement

import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.service.QuestionManagementService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class SettlementCalculator {
    fun determineWinningChoice(finalResult: FinalResult): Choice {
        return when (finalResult) {
            FinalResult.YES -> Choice.YES
            FinalResult.NO -> Choice.NO
            FinalResult.PENDING -> throw IllegalStateException("Cannot determine winner for PENDING result.")
        }
    }

    fun calculatePayout(betAmount: Long, totalPool: Long, winningPool: Long): Long {
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
}
