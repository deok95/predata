package com.predata.backend.sports.event

import org.springframework.context.ApplicationEvent

class MatchGoalEvent(
    source: Any,
    val matchId: Long,
    val homeScore: Int,
    val awayScore: Int,
    val minute: Int?
) : ApplicationEvent(source)
