package com.predata.backend.sports.provider.football

import com.fasterxml.jackson.databind.JsonNode
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter

@Component
class FootballDataProvider(
    @Value("\${sports.football-data.api-key:}") private val apiKey: String,
    @Value("\${sports.football-data.base-url:https://api.football-data.org/v4}") private val baseUrl: String,
    @Value("\${sports.football-data.fetch-window-days:7}") private val fetchWindowDays: Long,
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
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val fromDate = todayUtc.format(DateTimeFormatter.ISO_DATE)
        val toDate = todayUtc.plusDays(fetchWindowDays).format(DateTimeFormatter.ISO_DATE)

        // 1) 7일 범위 내 SCHEDULED 경기 조회
        val rangeUrl = "$baseUrl/competitions/$code/matches?status=SCHEDULED&dateFrom=$fromDate&dateTo=$toDate"
        val rangeRoot = try {
            objectMapper.readTree(callApi(rangeUrl))
        } catch (e: HttpClientErrorException) {
            logger.error("[FootballData] API error: ${e.statusCode} for $rangeUrl")
            return emptyList()
        } catch (e: Exception) {
            logger.error("[FootballData] Failed to fetch matches from $rangeUrl: ${e.message}")
            return emptyList()
        }

        // 2) 해당 범위에서 라운드(matchday) 추출
        val selectedMatchday = extractNearestMatchday(rangeRoot)
        if (selectedMatchday == null) {
            logger.info("[FootballData] No scheduled matches in next {} days", fetchWindowDays)
            return emptyList()
        }

        // 3) 가장 임박한 1개 라운드만 재조회
        val deduped = linkedMapOf<String, Match>()
        val roundUrl = "$baseUrl/competitions/$code/matches?status=SCHEDULED&matchday=$selectedMatchday&dateFrom=$fromDate&dateTo=$toDate"
        val roundMatches = fetchMatches(roundUrl, league)
        for (match in roundMatches) {
            val key = match.externalMatchId ?: "${match.homeTeam}_${match.awayTeam}_${match.matchTime}"
            deduped[key] = match
        }

        logger.info(
            "[FootballData] Upcoming window={}d, selectedRound={}, matches={}",
            fetchWindowDays, selectedMatchday, deduped.size
        )
        return deduped.values.sortedBy { it.matchTime }
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

    /**
     * SCHEDULED 목록에서 가장 임박한 경기의 matchday(라운드) 1개를 선택한다.
     */
    private fun extractNearestMatchday(root: JsonNode): Int? {
        val matchesNode = root.get("matches") ?: return null
        if (!matchesNode.isArray) return null

        val groupedByMatchday = matchesNode.mapNotNull { node ->
            val matchday = node.path("matchday").asInt(-1)
            if (matchday <= 0) return@mapNotNull null

            val kickoff = parseUtcDate(node.path("utcDate").asText(""))
            MatchdayKickoff(matchday, kickoff)
        }.groupBy { it.matchday }

        if (groupedByMatchday.isEmpty()) return null

        return groupedByMatchday.entries
            .map { (matchday, kickoffs) ->
                val nearestKickoff = kickoffs
                    .mapNotNull { it.kickoffUtc }
                    .minOrNull() ?: LocalDateTime.MAX
                MatchdayNearest(matchday, nearestKickoff)
            }
            .sortedWith(compareBy<MatchdayNearest> { it.nearestKickoffUtc }.thenBy { it.matchday })
            .firstOrNull()
            ?.matchday
    }

    private fun parseUtcDate(value: String): LocalDateTime? {
        if (value.isBlank()) return null
        return try {
            Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun callApi(url: String): String {
        val headers = HttpHeaders()
        headers.set("X-Auth-Token", apiKey)
        val entity = HttpEntity<String>(headers)
        val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)
        return response.body ?: throw RuntimeException("Empty response body from $url")
    }

    private data class MatchdayKickoff(
        val matchday: Int,
        val kickoffUtc: LocalDateTime?
    )

    private data class MatchdayNearest(
        val matchday: Int,
        val nearestKickoffUtc: LocalDateTime
    )
}
