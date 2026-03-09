package com.predata.backend.sports.provider.football

import com.fasterxml.jackson.databind.JsonNode
import com.predata.backend.sports.domain.League
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FootballDataMatchMapper {

    fun toMatch(node: JsonNode, league: League): Match {
        val id = node.path("fixture").path("id").asLong(0L)
        val utcDate = node.path("fixture").path("date").asText("")
        val status = node.path("fixture").path("status").path("short").asText("")
        val homeTeamName = node.path("teams").path("home").path("name").asText("Unknown")
        val awayTeamName = node.path("teams").path("away").path("name").asText("Unknown")
        val homeScore = node.path("goals").path("home").let {
            if (it.isNull || it.isMissingNode) null else it.asInt()
        }
        val awayScore = node.path("goals").path("away").let {
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
        "NS", "TBD", "PST" -> MatchStatus.SCHEDULED
        "1H", "2H", "ET", "P", "BT", "LIVE" -> MatchStatus.LIVE
        "HT" -> MatchStatus.HALFTIME
        "FT", "AET", "PEN" -> MatchStatus.FINISHED
        "POST" -> MatchStatus.POSTPONED
        "CANC", "ABD", "AWD", "WO", "INT", "SUSP" -> MatchStatus.CANCELLED
        else -> MatchStatus.SCHEDULED
    }
}
