package com.predata.backend.sports.provider.football

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.sports.domain.League
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.Sport
import com.predata.backend.sports.domain.SportsDataProvider
import com.predata.backend.sports.repository.LeagueRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.predata.backend.sports.domain.MatchStatus

@Component
class FootballDataProvider(
    @Value("\${sports.football-data.api-key:}") private val apiKey: String,
    @Value("\${sports.football-data.base-url:https://v3.football.api-sports.io}") private val baseUrl: String,
    @Value("\${sports.football-data.fetch-window-days:7}") private val fetchWindowDays: Long,
    private val leagueRepository: LeagueRepository
) : SportsDataProvider {

    private val logger = LoggerFactory.getLogger(FootballDataProvider::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    override val sport: Sport = Sport.FOOTBALL

    companion object {
        const val PROVIDER_NAME = "api-football"
    }

    override fun fetchUpcomingMatches(league: League): List<Match> {
        val leagueId = league.externalLeagueId ?: return emptyList()
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val fromDate = todayUtc.format(DateTimeFormatter.ISO_DATE)
        val toDate = todayUtc.plusDays(fetchWindowDays).format(DateTimeFormatter.ISO_DATE)
        val season = currentSeason(todayUtc)

        val url = "$baseUrl/fixtures?league=$leagueId&season=$season&from=$fromDate&to=$toDate"
        val allInRange = fetchMatches(url, league)
        val openMatches = allInRange.filter {
            it.matchStatus == MatchStatus.SCHEDULED ||
                it.matchStatus == MatchStatus.LIVE ||
                it.matchStatus == MatchStatus.HALFTIME
        }

        logger.info(
            "[FootballData] Upcoming window={}d, league={}, season={}, matches={}",
            fetchWindowDays, leagueId, season, openMatches.size
        )
        return openMatches.sortedBy { it.matchTime }
    }

    override fun fetchLiveMatches(league: League): List<Match> {
        val leagueId = league.externalLeagueId ?: return emptyList()
        val url = "$baseUrl/fixtures?live=$leagueId"
        return fetchMatches(url, league)
    }

    override fun fetchMatchResult(externalMatchId: String): Match? {
        val url = "$baseUrl/fixtures?id=$externalMatchId"
        return try {
            val response = callApi(url)
            val root = objectMapper.readTree(response)
            val node = root.path("response").firstOrNull()
            if (node == null || node.isMissingNode) {
                logger.warn("[FootballData] Empty fixture response for {}", externalMatchId)
                return null
            }
            val leagueId = node.path("league").path("id").asText("")
            if (leagueId.isBlank()) {
                logger.warn("[FootballData] No league id in match {} response", externalMatchId)
                return null
            }
            val league = leagueRepository.findByExternalLeagueIdAndProvider(leagueId, PROVIDER_NAME)
            if (league == null) {
                logger.warn("[FootballData] League not found for league id: {}", leagueId)
                return null
            }
            FootballDataMatchMapper.toMatch(node, league)
        } catch (e: HttpClientErrorException) {
            logger.error("[FootballData] API error for match $externalMatchId: ${e.statusCode}")
            null
        } catch (e: Exception) {
            logger.error("[FootballData] Failed to fetch match $externalMatchId: ${e.message}")
            null
        }
    }

    fun fetchFinishedMatches(league: League): List<Match> {
        val leagueId = league.externalLeagueId ?: return emptyList()
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val season = currentSeason(todayUtc)
        val url = "$baseUrl/fixtures?league=$leagueId&season=$season&status=FT"
        return fetchMatches(url, league).filter { it.matchStatus == com.predata.backend.sports.domain.MatchStatus.FINISHED }
    }

    private fun fetchMatches(url: String, league: League): List<Match> {
        return try {
            val response = callApi(url)
            val root = objectMapper.readTree(response)
            val matchesNode = root.get("response")
            if (matchesNode != null && matchesNode.isArray) {
                val matches = matchesNode.mapNotNull { node ->
                    FootballDataMatchMapper.toMatch(node, league)
                }
                logger.debug("[FootballData] Fetched ${matches.size} matches from $url")
                matches
            } else {
                emptyList()
            }
        } catch (e: HttpClientErrorException) {
            logger.error("[FootballData] API error: ${e.statusCode} for $url")
            emptyList()
        } catch (e: Exception) {
            logger.error("[FootballData] Failed to fetch matches from $url: ${e.message}")
            emptyList()
        }
    }

    private fun callApi(url: String): String {
        val headers = HttpHeaders()
        headers.set("x-apisports-key", apiKey)
        val entity = HttpEntity<String>(headers)
        val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)
        return response.body ?: throw RuntimeException("Empty response body from $url")
    }

    private fun currentSeason(nowUtc: LocalDate): Int =
        if (nowUtc.monthValue >= 7) nowUtc.year else nowUtc.year - 1
}
