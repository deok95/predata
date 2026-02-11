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

@Component
class FootballDataProvider(
    @Value("\${sports.football-data.api-key:}") private val apiKey: String,
    @Value("\${sports.football-data.base-url:https://api.football-data.org/v4}") private val baseUrl: String,
    private val leagueRepository: LeagueRepository
) : SportsDataProvider {

    private val logger = LoggerFactory.getLogger(FootballDataProvider::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    override val sport: Sport = Sport.FOOTBALL

    companion object {
        const val PROVIDER_NAME = "football-data.org"
    }

    override fun fetchUpcomingMatches(league: League): List<Match> {
        val code = league.externalLeagueId ?: return emptyList()
        val url = "$baseUrl/competitions/$code/matches?status=SCHEDULED"
        return fetchMatches(url, league)
    }

    override fun fetchLiveMatches(league: League): List<Match> {
        val code = league.externalLeagueId ?: return emptyList()
        val url = "$baseUrl/competitions/$code/matches?status=LIVE"
        return fetchMatches(url, league)
    }

    override fun fetchMatchResult(externalMatchId: String): Match? {
        val url = "$baseUrl/matches/$externalMatchId"
        return try {
            val response = callApi(url)
            val matchNode = objectMapper.readTree(response)
            val competitionCode = matchNode.path("competition").path("code").asText("")
            if (competitionCode.isBlank()) {
                logger.warn("[FootballData] No competition code in match $externalMatchId response")
                return null
            }
            val league = leagueRepository.findByExternalLeagueIdAndProvider(competitionCode, PROVIDER_NAME)
            if (league == null) {
                logger.warn("[FootballData] League not found for competition code: $competitionCode")
                return null
            }
            FootballDataMatchMapper.toMatch(matchNode, league)
        } catch (e: HttpClientErrorException) {
            logger.error("[FootballData] API error for match $externalMatchId: ${e.statusCode}")
            null
        } catch (e: Exception) {
            logger.error("[FootballData] Failed to fetch match $externalMatchId: ${e.message}")
            null
        }
    }

    fun fetchFinishedMatches(league: League): List<Match> {
        val code = league.externalLeagueId ?: return emptyList()
        val url = "$baseUrl/competitions/$code/matches?status=FINISHED"
        return fetchMatches(url, league)
    }

    private fun fetchMatches(url: String, league: League): List<Match> {
        return try {
            val response = callApi(url)
            val root = objectMapper.readTree(response)
            val matchesNode = root.get("matches")
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
        headers.set("X-Auth-Token", apiKey)
        val entity = HttpEntity<String>(headers)
        val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)
        return response.body ?: throw RuntimeException("Empty response body from $url")
    }
}
