package com.predata.backend.sports.domain

interface SportsDataProvider {

    val sport: Sport

    fun fetchUpcomingMatches(league: League): List<Match>

    fun fetchLiveMatches(league: League): List<Match>

    fun fetchMatchResult(externalMatchId: String): Match?
}
