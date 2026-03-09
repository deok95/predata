package com.predata.backend

import com.predata.backend.domain.policy.BotSchedulerPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BotSchedulerPolicyTest {
    @Test
    fun `scheduler only runs when enabled`() {
        assertTrue(BotSchedulerPolicy.shouldRun(true))
        assertFalse(BotSchedulerPolicy.shouldRun(false))
    }
}
