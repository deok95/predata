package com.predata.backend.service.settlement.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import com.predata.backend.service.MatchResult
import com.predata.backend.service.SportsApiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 스포츠 경기 결과 기반 정산 어댑터.
 *
 * resolutionSource 형식:
 *   sports://12345  |  match://12345  |  12345 (순수 숫자 — 하위 호환)
 *
 * resolutionRule 형식: HOME_WIN | AWAY_WIN | DRAW (대소문자 무시)
 *
 * 경기 미종료(status != FINISHED) 또는 데이터 미존재 시:
 *   result=null, confidence=0.0 → 자동정산 보류
 *
 * 경기 종료 + 규칙 판정 성공 시:
 *   result=YES/NO, confidence=1.0
 */
@Component
class SportsResolutionAdapter(
    private val sportsApiService: SportsApiService,
) : ResolutionAdapter {

    private val log = LoggerFactory.getLogger(SportsResolutionAdapter::class.java)
    private val objectMapper = ObjectMapper()

    private val NUMERIC_ID = Regex("""^\d+$""")

    override fun supports(marketType: MarketType): Boolean = marketType == MarketType.VERIFIABLE

    override fun supportsSource(resolutionSource: String?): Boolean {
        if (resolutionSource == null) return false
        return resolutionSource.startsWith("sports://") ||
               resolutionSource.startsWith("match://") ||
               resolutionSource.matches(NUMERIC_ID)
    }

    override fun resolve(question: Question): ResolutionResult {
        val source = question.resolutionSource
            ?: return pendingResult("resolutionSource is null")

        val matchId = parseMatchId(source)
            ?: return pendingResult("matchId parse failed: $source")

        val expectedOutcome = parseOutcome(question.resolutionRule)
            ?: return pendingResult("resolutionRule parse failed: '${question.resolutionRule}'")

        val matchResult = sportsApiService.fetchMatchResult(matchId)
            ?: return pendingResult("match data not available for matchId=$matchId")

        if (matchResult.status != "FINISHED") {
            return pendingResult("match not finished (status=${matchResult.status}, matchId=$matchId)")
        }

        val actualOutcome = matchResult.result
        val finalResult = if (actualOutcome == expectedOutcome) FinalResult.YES else FinalResult.NO

        log.info(
            "[SportsAdapter] matchId={} outcome={} expected={} → {}",
            matchId, actualOutcome, expectedOutcome, finalResult,
        )

        val sourceUrl = "https://v3.football.api-sports.io/fixtures?id=$matchId"
        val payload = buildPayload(matchId, matchResult, actualOutcome, expectedOutcome, finalResult)

        return ResolutionResult(
            result = finalResult,
            sourcePayload = payload,
            sourceUrl = sourceUrl,
            confidence = 1.0,
        )
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private fun parseMatchId(source: String): String? {
        val id = when {
            source.startsWith("sports://") -> source.removePrefix("sports://")
            source.startsWith("match://")  -> source.removePrefix("match://")
            source.matches(NUMERIC_ID)     -> source
            else                           -> return null
        }.trim()
        return id.ifBlank { null }
    }

    private fun parseOutcome(rule: String): String? = when (rule.trim().uppercase()) {
        "HOME_WIN" -> "HOME_WIN"
        "AWAY_WIN" -> "AWAY_WIN"
        "DRAW"     -> "DRAW"
        else       -> null
    }

    private fun buildPayload(
        matchId: String,
        matchResult: MatchResult,
        actualOutcome: String,
        expectedOutcome: String,
        finalResult: FinalResult,
    ): String = objectMapper.writeValueAsString(
        mapOf(
            "matchId"         to matchId,
            "status"          to matchResult.status,
            "homeScore"       to matchResult.homeScore,
            "awayScore"       to matchResult.awayScore,
            "actualOutcome"   to actualOutcome,
            "expectedOutcome" to expectedOutcome,
            "result"          to finalResult.name,
        )
    )

    private fun pendingResult(reason: String): ResolutionResult {
        log.info("[SportsAdapter] 보류: {}", reason)
        return ResolutionResult(result = null, sourcePayload = null, sourceUrl = null, confidence = 0.0)
    }
}
