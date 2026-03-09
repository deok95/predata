package com.predata.backend

import com.predata.backend.domain.DraftStatus
import com.predata.backend.domain.policy.DraftAccessResult
import com.predata.backend.domain.policy.DraftLifecyclePolicy
import com.predata.backend.domain.policy.DraftSubmitStateResult
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DraftLifecyclePolicyTest {
    @Test
    fun `evaluateAccess returns expected results`() {
        val now = LocalDateTime.now()
        assertEquals(
            DraftAccessResult.OK,
            DraftLifecyclePolicy.evaluateAccess(true, DraftStatus.OPEN, now.plusMinutes(1), now)
        )
        assertEquals(
            DraftAccessResult.NOT_FOUND,
            DraftLifecyclePolicy.evaluateAccess(false, DraftStatus.OPEN, now.plusMinutes(1), now)
        )
        assertEquals(
            DraftAccessResult.ALREADY_CONSUMED,
            DraftLifecyclePolicy.evaluateAccess(true, DraftStatus.CONSUMED, now.plusMinutes(1), now)
        )
        assertEquals(
            DraftAccessResult.EXPIRED,
            DraftLifecyclePolicy.evaluateAccess(true, DraftStatus.OPEN, now.minusMinutes(1), now)
        )
    }

    @Test
    fun `evaluateSubmitState handles expired and consumed`() {
        val now = LocalDateTime.now()
        assertEquals(
            DraftSubmitStateResult.OK,
            DraftLifecyclePolicy.evaluateSubmitState(DraftStatus.OPEN, now.plusMinutes(1), now)
        )
        assertEquals(
            DraftSubmitStateResult.ALREADY_CONSUMED,
            DraftLifecyclePolicy.evaluateSubmitState(DraftStatus.CONSUMED, now.plusMinutes(1), now)
        )
        assertEquals(
            DraftSubmitStateResult.EXPIRED_NEEDS_CLOSE,
            DraftLifecyclePolicy.evaluateSubmitState(DraftStatus.OPEN, now.minusMinutes(1), now)
        )
    }

    @Test
    fun `ownershipAndKeyValid requires both ownership and key match`() {
        assertTrue(DraftLifecyclePolicy.ownershipAndKeyValid(true, true))
        assertFalse(DraftLifecyclePolicy.ownershipAndKeyValid(true, false))
    }
}
