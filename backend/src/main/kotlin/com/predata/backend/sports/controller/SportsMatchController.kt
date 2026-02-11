package com.predata.backend.sports.controller

import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.service.SportsMatchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sports/matches")
class SportsMatchController(
    private val sportsMatchService: SportsMatchService
) {

    @GetMapping
    fun getMatches(
        @RequestParam league: String,
        @RequestParam status: String
    ): ResponseEntity<List<MatchResponse>> {
        val matches = when (status.uppercase()) {
            "SCHEDULED" -> sportsMatchService.getUpcomingMatches(league)
            "LIVE" -> sportsMatchService.getLiveMatches(league)
            "FINISHED" -> sportsMatchService.getFinishedMatches(league)
            else -> throw IllegalArgumentException("Invalid status: $status. Must be SCHEDULED, LIVE, or FINISHED")
        }
        return ResponseEntity.ok(matches.map { it.toResponse() })
    }
}

data class MatchResponse(
    val id: Long?,
    val leagueCode: String?,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val matchStatus: String,
    val matchTime: String,
    val externalMatchId: String?,
    val provider: String?
)

private fun Match.toResponse() = MatchResponse(
    id = this.id,
    leagueCode = this.league.externalLeagueId,
    homeTeam = this.homeTeam,
    awayTeam = this.awayTeam,
    homeScore = this.homeScore,
    awayScore = this.awayScore,
    matchStatus = this.matchStatus.name,
    matchTime = this.matchTime.toString(),
    externalMatchId = this.externalMatchId,
    provider = this.provider
)
