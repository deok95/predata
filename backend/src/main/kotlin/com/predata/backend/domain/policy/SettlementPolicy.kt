package com.predata.backend.domain.policy

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.exception.SettlementDelayActiveException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

object SettlementPolicy {
    private const val DELAYED_SETTLEMENT_HOURS = 12L

    fun ensureCanInitiate(status: QuestionStatus) {
        if (status == QuestionStatus.SETTLED) {
            throw IllegalStateException("Question already settled.")
        }
        if (status != QuestionStatus.VOTING && status != QuestionStatus.BETTING && status != QuestionStatus.BREAK) {
            throw IllegalStateException("Question status is not eligible for settlement. (current: $status)")
        }
    }

    fun ensureCanFinalize(
        status: QuestionStatus,
        disputeDeadline: LocalDateTime?,
        nowUtc: LocalDateTime,
        skipDeadlineCheck: Boolean,
    ) {
        if (status != QuestionStatus.SETTLED) {
            throw IllegalStateException("Only pending settlement questions can be finalized. (current: $status)")
        }
        if (disputeDeadline == null) {
            throw IllegalStateException("Settlement already finalized.")
        }
        if (!skipDeadlineCheck && nowUtc.isBefore(disputeDeadline)) {
            throw SettlementDelayActiveException("정산 대기 시간이 아직 종료되지 않았습니다. (deadline: $disputeDeadline)")
        }
    }

    fun resolveDelayHours(marketType: MarketType, hasExternalEvidence: Boolean): Long {
        return if (hasExternalEvidence && marketType == MarketType.VERIFIABLE) 0L else DELAYED_SETTLEMENT_HOURS
    }

    fun resolveWinningSide(finalResult: FinalResult): ShareOutcome? {
        return when (finalResult) {
            FinalResult.YES -> ShareOutcome.YES
            FinalResult.NO -> ShareOutcome.NO
            FinalResult.PENDING -> null
        }
    }

    fun payoutForWinningShares(shares: BigDecimal): BigDecimal =
        shares.setScale(18, RoundingMode.DOWN)

    fun nextCollateralLocked(collateralLocked: BigDecimal, totalPayout: BigDecimal): BigDecimal {
        val newLocked = collateralLocked.subtract(totalPayout).setScale(18, RoundingMode.DOWN)
        require(newLocked >= BigDecimal.ZERO) {
            "Insolvency error: payout=$totalPayout > collateralLocked=$collateralLocked"
        }
        return newLocked
    }
}
