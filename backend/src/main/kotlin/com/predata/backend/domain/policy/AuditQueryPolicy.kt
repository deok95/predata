package com.predata.backend.domain.policy

import com.predata.backend.domain.AuditAction
import java.time.LocalDateTime

enum class AuditQueryRoute {
    MEMBER_ACTION_TIME_RANGE,
    MEMBER_ONLY,
    ACTION_ONLY,
    TIME_RANGE_ONLY,
    ALL,
}

object AuditQueryPolicy {
    fun route(
        memberId: Long?,
        action: AuditAction?,
        from: LocalDateTime?,
        to: LocalDateTime?,
    ): AuditQueryRoute = when {
        memberId != null && action != null && from != null && to != null -> AuditQueryRoute.MEMBER_ACTION_TIME_RANGE
        memberId != null -> AuditQueryRoute.MEMBER_ONLY
        action != null -> AuditQueryRoute.ACTION_ONLY
        from != null && to != null -> AuditQueryRoute.TIME_RANGE_ONLY
        else -> AuditQueryRoute.ALL
    }
}
