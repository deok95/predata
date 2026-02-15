package com.predata.backend.service.settlement

import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.QuestionType
import com.predata.backend.service.QuestionManagementService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class OpinionSettlementPolicy : SettlementPolicy {
    override fun supports(questionType: QuestionType): Boolean = questionType == QuestionType.OPINION

    override fun determineWinningChoice(finalResult: FinalResult): Choice {
        return if (finalResult == FinalResult.YES) Choice.YES else Choice.NO
    }

    override fun calculatePayout(betAmount: Long, totalPool: Long, winningPool: Long): Long {
        val initialLiquidity = QuestionManagementService.INITIAL_LIQUIDITY
        val effectiveTotalPool = maxOf(0L, totalPool - initialLiquidity * 2)
        val effectiveWinningPool = maxOf(0L, winningPool - initialLiquidity)

        if (effectiveWinningPool == 0L) return betAmount

        // 현재는 VERIFIABLE과 동일 정책을 사용한다.
        // 향후 OPINION 전용 수수료/가중치 정책으로 쉽게 교체 가능.
        val payoutRatio = BigDecimal(effectiveTotalPool)
            .divide(BigDecimal(effectiveWinningPool), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal("0.99"))

        return BigDecimal(betAmount)
            .multiply(payoutRatio)
            .setScale(0, RoundingMode.DOWN)
            .toLong()
    }
}
