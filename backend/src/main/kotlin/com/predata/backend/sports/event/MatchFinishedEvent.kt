package com.predata.backend.sports.event

import org.springframework.context.ApplicationEvent

class MatchFinishedEvent(
    source: Any,
    val matchId: Long,
    val homeScore: Int,
    val awayScore: Int
) : ApplicationEvent(source)
