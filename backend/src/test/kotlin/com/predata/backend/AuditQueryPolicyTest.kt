package com.predata.backend

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.policy.AuditQueryPolicy
import com.predata.backend.domain.policy.AuditQueryRoute
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AuditQueryPolicyTest {
    @Test
    fun `route selects most specific filter first`() {
        val from = LocalDateTime.now().minusDays(1)
        val to = LocalDateTime.now()

        assertEquals(
            AuditQueryRoute.MEMBER_ACTION_TIME_RANGE,
            AuditQueryPolicy.route(memberId = 1L, action = AuditAction.SETTLE, from = from, to = to)
        )
    }

    @Test
    fun `route selects member action time and fallback routes`() {
        val from = LocalDateTime.now().minusDays(1)
        val to = LocalDateTime.now()
        assertEquals(AuditQueryRoute.MEMBER_ONLY, AuditQueryPolicy.route(1L, null, null, null))
        assertEquals(AuditQueryRoute.ACTION_ONLY, AuditQueryPolicy.route(null, AuditAction.SETTLE, null, null))
        assertEquals(AuditQueryRoute.TIME_RANGE_ONLY, AuditQueryPolicy.route(null, null, from, to))
        assertEquals(AuditQueryRoute.ALL, AuditQueryPolicy.route(null, null, null, null))
    }
}
