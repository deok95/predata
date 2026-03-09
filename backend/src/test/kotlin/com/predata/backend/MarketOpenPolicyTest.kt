package com.predata.backend

import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.policy.MarketOpenDecision
import com.predata.backend.domain.policy.MarketOpenPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketOpenPolicyTest {
    @Test
    fun `decideOpen returns OPEN_AND_TRANSITION for BREAK`() {
        assertEquals(MarketOpenDecision.OPEN_AND_TRANSITION, MarketOpenPolicy.decideOpen(QuestionStatus.BREAK))
    }

    @Test
    fun `decideOpen returns success for betting and failure for others`() {
        assertEquals(MarketOpenDecision.SUCCESS_ALREADY_BETTING, MarketOpenPolicy.decideOpen(QuestionStatus.BETTING))
        assertEquals(MarketOpenDecision.FAILURE_INVALID_STATUS, MarketOpenPolicy.decideOpen(QuestionStatus.CANCELLED))
    }

    @Test
    fun `isPoolAlreadyExistsRecoverable checks message and status`() {
        assertTrue(
            MarketOpenPolicy.isPoolAlreadyExistsRecoverable(
                errorMessage = "Market pool already exists for this question.",
                currentStatus = QuestionStatus.BREAK
            )
        )
        assertFalse(
            MarketOpenPolicy.isPoolAlreadyExistsRecoverable(
                errorMessage = "any other error",
                currentStatus = QuestionStatus.BREAK
            )
        )
    }
}
