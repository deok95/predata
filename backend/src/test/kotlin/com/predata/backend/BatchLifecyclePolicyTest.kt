package com.predata.backend

import com.predata.backend.domain.market.BatchStatus
import com.predata.backend.domain.policy.BatchLifecyclePolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchLifecyclePolicyTest {
    @Test
    fun `terminal status detection works`() {
        assertTrue(BatchLifecyclePolicy.isTerminal(BatchStatus.COMPLETED))
        assertTrue(BatchLifecyclePolicy.isTerminal(BatchStatus.PARTIAL_FAILED))
        assertTrue(BatchLifecyclePolicy.isTerminal(BatchStatus.FAILED))
        assertFalse(BatchLifecyclePolicy.isTerminal(BatchStatus.OPENING))
    }

    @Test
    fun `finalStatus returns completed failed or partial`() {
        assertEquals(BatchStatus.COMPLETED, BatchLifecyclePolicy.finalStatus(opened = 3, failed = 0))
        assertEquals(BatchStatus.FAILED, BatchLifecyclePolicy.finalStatus(opened = 0, failed = 2))
        assertEquals(BatchStatus.PARTIAL_FAILED, BatchLifecyclePolicy.finalStatus(opened = 1, failed = 1))
    }

    @Test
    fun `openResultSummary returns normalized summary code format`() {
        assertEquals(null, BatchLifecyclePolicy.openResultSummary(opened = 2, failed = 0))
        assertEquals(
            "BATCH_OPEN_FAILED: opened=0, failed=2",
            BatchLifecyclePolicy.openResultSummary(opened = 0, failed = 2)
        )
        assertEquals(
            "BATCH_PARTIAL_FAILED: opened=1, failed=1",
            BatchLifecyclePolicy.openResultSummary(opened = 1, failed = 1)
        )
    }
}
