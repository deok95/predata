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
    @Value("\${sports.api.enabled:false}") private val apiEnabled: Boolean
) {

    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()
    
    // API-FOOTBALL 기본 URL
    private val footballApiUrl = "https://v3.football.api-sports.io"
    
    // 프리미어 리그만 가져오기
    private val importantLeagues = mapOf(
        "EPL" to 39  // Premier League (England)
    )

    /**
     * 다가오는 경기 가져오기 (향후 14일) - EPL 경기가 많지 않으므로 2주
     */
    fun fetchUpcomingMatches(): List<SportsMatch> {
        if (!apiEnabled || apiKey.isBlank()) {
            println("[SportsAPI] API가 비활성화되어 있거나 키가 없습니다.")
            return emptyList()
        }

        val matches = mutableListOf<SportsMatch>()
        val today = LocalDateTime.now()
        val nextWeek = today.plusDays(14)  // 2주로 확장
        
        val dateFrom = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = nextWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)

        try {
            for ((leagueName, leagueId) in importantLeagues) {
                println("[SportsAPI] Fetching matches for $leagueName (ID: $leagueId)")
                
                val url = "$footballApiUrl/fixtures?league=$leagueId&season=2026&from=$dateFrom&to=$dateTo"
                
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
                        matches.add(parseFixtureToMatch(fixture, leagueName))
                    }
                }
                
                // EPL만 가져오므로 대기 불필요
            }
        } catch (e: HttpClientErrorException) {
            println("[SportsAPI] API 호출 실패: ${e.message}")
        } catch (e: Exception) {
            println("[SportsAPI] 오류 발생: ${e.message}")
        }

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
