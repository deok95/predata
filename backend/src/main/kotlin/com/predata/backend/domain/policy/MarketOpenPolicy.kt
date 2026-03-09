package com.predata.backend.domain.policy

import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.market.OpenStatus

enum class MarketOpenDecision {
    SUCCESS_ALREADY_BETTING,
    FAILURE_INVALID_STATUS,
    OPEN_AND_TRANSITION,
}

object MarketOpenPolicy {
    fun decideOpen(questionStatus: QuestionStatus): MarketOpenDecision = when (questionStatus) {
        QuestionStatus.BETTING -> MarketOpenDecision.SUCCESS_ALREADY_BETTING
        QuestionStatus.BREAK -> MarketOpenDecision.OPEN_AND_TRANSITION
        else -> MarketOpenDecision.FAILURE_INVALID_STATUS
    }

    fun isPoolAlreadyExistsRecoverable(errorMessage: String?, currentStatus: QuestionStatus): Boolean {
        return errorMessage?.contains("Market pool already exists") == true && currentStatus == QuestionStatus.BREAK
    }

    fun truncateOpenError(error: String): String = error.take(1000)

    fun nextOpenStatus(success: Boolean): OpenStatus = if (success) OpenStatus.OPENED else OpenStatus.OPEN_FAILED
}
