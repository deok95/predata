package com.predata.backend.sports.service

import com.predata.backend.exception.NotFoundException
import com.predata.backend.sports.domain.League
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.provider.football.FootballDataProvider
import com.predata.backend.sports.repository.LeagueRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SportsMatchService(
    private val footballDataProvider: FootballDataProvider,
    private val leagueRepository: LeagueRepository
) {

    private val logger = LoggerFactory.getLogger(SportsMatchService::class.java)

    @Cacheable(value = ["sportsMatches"], key = "'upcoming_' + #leagueCode")
    fun getUpcomingMatches(leagueCode: String): List<Match> {
        val league = resolveLeague(leagueCode)
        return footballDataProvider.fetchUpcomingMatches(league)
    }

    @Cacheable(value = ["sportsMatches"], key = "'live_' + #leagueCode")
    fun getLiveMatches(leagueCode: String): List<Match> {
        val league = resolveLeague(leagueCode)
        return footballDataProvider.fetchLiveMatches(league)
    }

    @Cacheable(value = ["sportsMatches"], key = "'finished_' + #leagueCode")
    fun getFinishedMatches(leagueCode: String): List<Match> {
        val league = resolveLeague(leagueCode)
        return footballDataProvider.fetchFinishedMatches(league)
    }

    fun getMatchResult(externalMatchId: String): Match {
        return footballDataProvider.fetchMatchResult(externalMatchId)
            ?: throw NotFoundException("Match not found: $externalMatchId")
    }

    private fun resolveLeague(leagueCode: String): League {
        return leagueRepository.findByExternalLeagueIdAndProvider(
            leagueCode, FootballDataProvider.PROVIDER_NAME
        ) ?: throw NotFoundException("League not found: $leagueCode")
    }
}
