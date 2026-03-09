package com.predata.backend

import com.predata.backend.domain.policy.QuestionQuotaPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionQuotaPolicyUnitTest {
    @Test
    fun `admin role bypasses quota`() {
        assertTrue(QuestionQuotaPolicy.shouldBypassQuota("ADMIN"))
        assertTrue(QuestionQuotaPolicy.shouldBypassQuota("admin"))
        assertFalse(QuestionQuotaPolicy.shouldBypassQuota("USER"))
    }

    @Test
    fun `daily limit and active lock checks are deterministic`() {
        assertTrue(QuestionQuotaPolicy.exceedsDailyCreateLimit(1))
        assertFalse(QuestionQuotaPolicy.exceedsDailyCreateLimit(0))
        assertTrue(QuestionQuotaPolicy.shouldBlockForActiveQuestion(true))
        assertFalse(QuestionQuotaPolicy.shouldBlockForActiveQuestion(false))
    }
}
