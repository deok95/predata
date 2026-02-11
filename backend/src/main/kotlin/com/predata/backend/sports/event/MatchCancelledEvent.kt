package com.predata.backend.sports.event

import com.predata.backend.sports.domain.MatchStatus
import org.springframework.context.ApplicationEvent

class MatchCancelledEvent(
    source: Any,
    val matchId: Long,
    val matchStatus: MatchStatus
) : ApplicationEvent(source)
