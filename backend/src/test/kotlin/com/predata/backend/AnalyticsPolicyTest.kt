package com.predata.backend

import com.predata.backend.domain.policy.AnalyticsPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsPolicyTest {
    @Test
    fun `percentage handles zero denominator`() {
        assertEquals(0.0, AnalyticsPolicy.percentage(10, 0))
        assertEquals(50.0, AnalyticsPolicy.percentage(1, 2))
    }

    @Test
    fun `qualityScore decreases with bigger gap`() {
        val lowGap = AnalyticsPolicy.qualityScore(2.0)
        val highGap = AnalyticsPolicy.qualityScore(30.0)
        assertTrue(lowGap > highGap)
    }

    @Test
    fun `suspicious latency threshold works`() {
        assertTrue(AnalyticsPolicy.isSuspiciousLatency(100))
        assertEquals(false, AnalyticsPolicy.isSuspiciousLatency(3000))
    }
}
