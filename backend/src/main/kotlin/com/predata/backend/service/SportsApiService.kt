package com.predata.backend.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.SportsMatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SportsApiService(
    @Value("\${sports.api.key:}") private val apiKey: String,
    @Value("\${sports.api.enabled:false}") private val apiEnabled: Boolean,
    @Value("\${sports.api.rate-limit-delay-ms:500}") private val rateLimitDelayMs: Long,
    @Value("\${sports.api.fetch-window-days:14}") private val fetchWindowDays: Long
) {

    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    // API-FOOTBALL 기본 URL
    private val footballApiUrl = "https://v3.football.api-sports.io"

    /**
     * 리그 설정 데이터 클래스
     * 향후 다종목 확장 시 SportProvider 인터페이스로 추출 예정
     */
    data class LeagueConfig(
        val displayName: String,
        val apiLeagueId: Int,
        val season: Int,
        val matchDurationHours: Long = 2
    )

    // 지원 축구 리그 목록
    private val footballLeagues = listOf(
        LeagueConfig("EPL", 39, 2025),            // Premier League (England) - 2025-26 시즌
        LeagueConfig("La Liga", 140, 2025),        // La Liga (Spain)
        LeagueConfig("Serie A", 135, 2025),        // Serie A (Italy)
        LeagueConfig("Bundesliga", 78, 2025),      // Bundesliga (Germany)
        LeagueConfig("Ligue 1", 61, 2025),         // Ligue 1 (France)
        LeagueConfig("K-League", 292, 2026),       // K-League 1 (South Korea) - 캘린더 연도
        LeagueConfig("UCL", 2, 2025),              // UEFA Champions League
    )

    /**
     * 다가오는 경기 가져오기 (설정된 기간 내) - 다중 리그 지원
     */
    fun fetchUpcomingMatches(): List<SportsMatch> {
        if (!apiEnabled || apiKey.isBlank()) {
            println("[SportsAPI] API가 비활성화되어 있거나 키가 없습니다.")
            return emptyList()
        }

        val matches = mutableListOf<SportsMatch>()
        val today = LocalDateTime.now()
        val futureDate = today.plusDays(fetchWindowDays)

        val dateFrom = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        try {
            for ((index, league) in footballLeagues.withIndex()) {
                println("[SportsAPI] Fetching matches for ${league.displayName} (ID: ${league.apiLeagueId}, Season: ${league.season})")

                val url = "$footballApiUrl/fixtures?league=${league.apiLeagueId}&season=${league.season}&from=$dateFrom&to=$dateTo"

                val headers = org.springframework.http.HttpHeaders()
                headers.set("x-rapidapi-key", apiKey)
                headers.set("x-rapidapi-host", "v3.football.api-sports.io")

                val entity = org.springframework.http.HttpEntity<String>(headers)
                val response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String::class.java
                )

                val jsonNode = objectMapper.readTree(response.body)
                val fixtures = jsonNode.get("response")

                if (fixtures != null && fixtures.isArray) {
                    for (fixture in fixtures) {
                        matches.add(parseFixtureToMatch(fixture, league.displayName))
                    }
                    println("[SportsAPI] ${league.displayName}: ${fixtures.size()}건 가져옴")
                }

                // 다중 리그 API 호출 간 rate limit 준수 (마지막 리그 이후에는 대기 불필요)
                if (index < footballLeagues.size - 1) {
                    Thread.sleep(rateLimitDelayMs)
                }
            }
        } catch (e: HttpClientErrorException) {
            println("[SportsAPI] API 호출 실패: ${e.message}")
        } catch (e: Exception) {
            println("[SportsAPI] 오류 발생: ${e.message}")
        }

        println("[SportsAPI] 총 ${matches.size}건의 경기를 가져왔습니다.")
        return matches
    }

    /**
     * 특정 경기의 결과 가져오기
     */
    fun fetchMatchResult(externalApiId: String): MatchResult? {
        if (!apiEnabled || apiKey.isBlank()) {
            return null
        }

        try {
            val url = "$footballApiUrl/fixtures?id=$externalApiId"
            
            val headers = org.springframework.http.HttpHeaders()
            headers.set("x-rapidapi-key", apiKey)
            headers.set("x-rapidapi-host", "v3.football.api-sports.io")
            
            val entity = org.springframework.http.HttpEntity<String>(headers)
            val response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String::class.java
            )

            val jsonNode = objectMapper.readTree(response.body)
            val fixture = jsonNode.get("response")?.get(0)
            
            if (fixture != null) {
                return parseMatchResult(fixture)
            }
        } catch (e: Exception) {
            println("[SportsAPI] 경기 결과 조회 실패: ${e.message}")
        }

        return null
    }

    /**
     * API 응답을 SportsMatch로 변환
     */
    private fun parseFixtureToMatch(fixture: JsonNode, leagueName: String): SportsMatch {
        val fixtureId = fixture.get("fixture").get("id").asText()
        val timestamp = fixture.get("fixture").get("timestamp").asLong()
        val matchDate = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(timestamp),
            ZoneId.systemDefault()
        )
        
        val homeTeam = fixture.get("teams").get("home").get("name").asText()
        val awayTeam = fixture.get("teams").get("away").get("name").asText()
        val status = fixture.get("fixture").get("status").get("short").asText()

        return SportsMatch(
            externalApiId = fixtureId,
            sportType = "FOOTBALL",
            leagueName = leagueName,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            matchDate = matchDate,
            status = when (status) {
                "NS" -> "SCHEDULED"  // Not Started
                "LIVE", "1H", "HT", "2H" -> "LIVE"
                "FT", "AET", "PEN" -> "FINISHED"  // Full Time, After Extra Time, Penalties
                else -> "SCHEDULED"
            }
        )
    }

    /**
     * 경기 결과 파싱
     */
    private fun parseMatchResult(fixture: JsonNode): MatchResult {
        val homeScore = fixture.get("goals").get("home").asInt()
        val awayScore = fixture.get("goals").get("away").asInt()
        val status = fixture.get("fixture").get("status").get("short").asText()

        val result = when {
            homeScore > awayScore -> "HOME_WIN"
            awayScore > homeScore -> "AWAY_WIN"
            else -> "DRAW"
        }

        return MatchResult(
            homeScore = homeScore,
            awayScore = awayScore,
            result = result,
            status = when (status) {
                "FT", "AET", "PEN" -> "FINISHED"
                else -> "LIVE"
            }
        )
    }
}

data class MatchResult(
    val homeScore: Int,
    val awayScore: Int,
    val result: String,
    val status: String
)
