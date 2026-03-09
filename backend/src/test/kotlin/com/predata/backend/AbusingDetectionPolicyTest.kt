package com.predata.backend

import com.predata.backend.domain.policy.AbusingDetectionPolicy
import com.predata.backend.dto.RiskLevel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbusingDetectionPolicyTest {
    @Test
    fun `classifyGapRisk follows threshold boundaries`() {
        assertEquals(RiskLevel.LOW, AbusingDetectionPolicy.classifyGapRisk(10.0))
        assertEquals(RiskLevel.MEDIUM, AbusingDetectionPolicy.classifyGapRisk(20.0))
        assertEquals(RiskLevel.HIGH, AbusingDetectionPolicy.classifyGapRisk(40.0))
        assertEquals(RiskLevel.CRITICAL, AbusingDetectionPolicy.classifyGapRisk(60.0))
    }

    @Test
    fun `fast clickers use latency and size thresholds`() {
        assertTrue(AbusingDetectionPolicy.isFastClicker(999))
        assertFalse(AbusingDetectionPolicy.isFastClicker(1000))
        assertFalse(AbusingDetectionPolicy.isFastClicker(null))
        assertFalse(AbusingDetectionPolicy.shouldIncludeFastClickers(500))
        assertTrue(AbusingDetectionPolicy.shouldIncludeFastClickers(501))
    }

    @Test
    fun `recommendation prioritizes critical then high then gap`() {
        val critical = AbusingDetectionPolicy.recommendation(criticalCount = 1, highCount = 0, overallGap = 5.0)
        val high = AbusingDetectionPolicy.recommendation(criticalCount = 0, highCount = 3, overallGap = 5.0)
        val gap = AbusingDetectionPolicy.recommendation(criticalCount = 0, highCount = 1, overallGap = 25.0)

        assertTrue(critical.contains("CRITICAL"))
        assertTrue(high.contains("HIGH"))
        assertTrue(gap.contains("Latency"))
    }
}
