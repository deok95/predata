package com.predata.backend.sports.provider.football

import com.fasterxml.jackson.databind.JsonNode
import com.predata.backend.sports.domain.League
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FootballDataMatchMapper {

    fun toMatch(node: JsonNode, league: League): Match {
        val id = node.get("id").asLong()
        val utcDate = node.get("utcDate").asText()
        val status = node.get("status").asText()
        val homeTeamName = node.path("homeTeam").path("name").asText("Unknown")
        val awayTeamName = node.path("awayTeam").path("name").asText("Unknown")
        val homeScore = node.path("score").path("fullTime").path("home").let {
            if (it.isNull || it.isMissingNode) null else it.asInt()
        }
        val awayScore = node.path("score").path("fullTime").path("away").let {
            if (it.isNull || it.isMissingNode) null else it.asInt()
        }

        val matchTime = LocalDateTime.parse(utcDate, DateTimeFormatter.ISO_DATE_TIME)

        return Match(
            league = league,
            homeTeam = homeTeamName,
            awayTeam = awayTeamName,
            homeScore = homeScore,
            awayScore = awayScore,
            matchStatus = mapStatus(status),
            matchTime = matchTime,
            externalMatchId = id.toString(),
            provider = FootballDataProvider.PROVIDER_NAME
        )
    }

    fun mapStatus(externalStatus: String): MatchStatus = when (externalStatus) {
        "SCHEDULED", "TIMED" -> MatchStatus.SCHEDULED
        "IN_PLAY" -> MatchStatus.LIVE
        "PAUSED" -> MatchStatus.HALFTIME
        "FINISHED" -> MatchStatus.FINISHED
        "POSTPONED" -> MatchStatus.POSTPONED
        "CANCELLED", "SUSPENDED" -> MatchStatus.CANCELLED
        else -> MatchStatus.SCHEDULED
    }
}
