package com.predata.backend

import com.predata.backend.domain.policy.SybilDetectionPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SybilDetectionPolicyTest {
    @Test
    fun `ip threshold and uniform pattern rules apply`() {
        assertFalse(SybilDetectionPolicy.shouldFlagSuspiciousIp(4))
        assertTrue(SybilDetectionPolicy.shouldFlagSuspiciousIp(5))

        assertFalse(SybilDetectionPolicy.isUniformChoicePattern(totalVotes = 4, uniqueChoiceCount = 1))
        assertFalse(SybilDetectionPolicy.isUniformChoicePattern(totalVotes = 5, uniqueChoiceCount = 2))
        assertTrue(SybilDetectionPolicy.isUniformChoicePattern(totalVotes = 5, uniqueChoiceCount = 1))
    }

    @Test
    fun `detail messages are stable`() {
        assertEquals("동일 IP에서 다수 계정 투표", SybilDetectionPolicy.ipReason())
        assertEquals("IP 127.0.0.1 에서 7개 계정이 투표", SybilDetectionPolicy.ipDetail("127.0.0.1", 7))
        assertEquals("동일 선택 패턴 (YES만 선택)", SybilDetectionPolicy.uniformChoiceReason("YES"))
    }
}
