package com.predata.backend.service.sports

import com.predata.backend.domain.SportsMatch
import com.predata.backend.service.MatchResult
import com.predata.backend.service.SportsApiService
import org.springframework.stereotype.Component

/**
 * 축구 종목 프로바이더.
 * 기존 SportsApiService에 위임하여 API-FOOTBALL 데이터를 제공.
 * 향후 SportsApiService의 로직을 이 클래스로 완전히 이전 가능.
 */
@Component
class FootballProvider(
    private val sportsApiService: SportsApiService
) : SportProvider {

    override val sportType = "FOOTBALL"

    override fun fetchUpcomingMatches(): List<SportsMatch> =
        sportsApiService.fetchUpcomingMatches()

    override fun fetchMatchResult(externalApiId: String): MatchResult? =
        sportsApiService.fetchMatchResult(externalApiId)

    override fun generateQuestionTitle(match: SportsMatch): String =
        "[${match.leagueName}] Will ${match.homeTeam} beat ${match.awayTeam}?"

    override fun getMatchDurationHours(): Long = 2

    override fun supportsLiveTracking(): Boolean = true

    override fun determineSettlement(matchResult: String): String = when (matchResult) {
        "HOME_WIN" -> "YES"
        "AWAY_WIN", "DRAW" -> "NO"
        else -> "UNKNOWN"
    }
}
