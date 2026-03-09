package com.predata.backend

import com.predata.backend.domain.policy.SybilGuardPolicy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SybilGuardPolicyTest {
    @Test
    fun `reward eligibility requires both account age and vote history`() {
        assertFalse(SybilGuardPolicy.isEligibleForReward(2, 10, minAccountAgeDays = 3, minVoteHistory = 3))
        assertFalse(SybilGuardPolicy.isEligibleForReward(10, 2, minAccountAgeDays = 3, minVoteHistory = 3))
        assertTrue(SybilGuardPolicy.isEligibleForReward(3, 3, minAccountAgeDays = 3, minVoteHistory = 3))
    }

    @Test
    fun `account age days is computed from createdAt to now`() {
        val createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        val now = LocalDateTime.of(2026, 1, 5, 0, 0)
        val days = SybilGuardPolicy.accountAgeDays(createdAt, now)
        assertTrue(days >= 4)
    }
}
