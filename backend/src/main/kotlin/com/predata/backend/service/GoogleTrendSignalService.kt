package com.predata.backend.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.TrendSignal
import com.predata.backend.dto.TrendSignalDto
import com.predata.backend.repository.TrendSignalRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@Service
class GoogleTrendSignalService(
    private val trendSignalRepository: TrendSignalRepository,
    @Value("\${app.questiongen.trends-url:https://trends.google.com/trends/api/dailytrends}")
    private val trendsUrl: String
) {

    private val logger = LoggerFactory.getLogger(GoogleTrendSignalService::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    @Transactional
    fun fetchAndStoreSignals(
        subcategory: String,
        targetDate: LocalDate,
        region: String,
        maxSignals: Int = 10
    ): List<TrendSignalDto> {
        // 트렌드 캐시 조회 시 region도 포함
        val existing = trendSignalRepository.findBySignalDateAndSubcategoryAndRegionOrderByTrendScoreDesc(targetDate, subcategory, region)
        if (existing.isNotEmpty()) {
            return existing.take(maxSignals).map {
                TrendSignalDto(
                    subcategory = it.subcategory,
                    keyword = it.keyword,
                    trendScore = it.trendScore,
                    region = it.region,
                    source = it.source
                )
            }
        }

        val signals = try {
            fetchFromGoogleTrends(subcategory, region, maxSignals)
        } catch (e: Exception) {
            logger.warn("[Trend] Google Trends fetch failed, using fallback: {}", e.message)
            fallbackSignals(subcategory, region, maxSignals)
        }

        if (signals.isNotEmpty()) {
            trendSignalRepository.saveAll(
                signals.map {
                    TrendSignal(
                        signalDate = targetDate,
                        subcategory = it.subcategory,
                        keyword = it.keyword,
                        trendScore = it.trendScore,
                        region = it.region,
                        source = it.source
                    )
                }
            )
        }

        return signals
    }

    private fun fetchFromGoogleTrends(subcategory: String, region: String, maxSignals: Int): List<TrendSignalDto> {
        val url = "$trendsUrl?hl=ko&tz=0&geo=$region"
        val response = restTemplate.exchange(url, HttpMethod.GET, null, String::class.java).body.orEmpty()
        if (response.isBlank()) {
            return fallbackSignals(subcategory, region, maxSignals)
        }

        val normalized = response.removePrefix(")]}'").trimStart('\n')
        val root = objectMapper.readTree(normalized)
        val searches = root
            .path("default")
            .path("trendingSearchesDays")
            .firstOrNull()
            ?.path("trendingSearches")

        if (searches == null || !searches.isArray) {
            return fallbackSignals(subcategory, region, maxSignals)
        }

        val mapped = mutableListOf<TrendSignalDto>()
        var rankScore = 100
        for (search in searches) {
            val keyword = search.path("title").path("query").asText("").trim()
            if (keyword.isBlank()) continue

            if (!belongsToSubcategory(keyword, subcategory)) continue

            mapped += TrendSignalDto(
                subcategory = subcategory,
                keyword = keyword,
                trendScore = rankScore.coerceAtLeast(1),
                region = region,
                source = "GOOGLE_TRENDS"
            )
            rankScore -= 5
            if (mapped.size >= maxSignals) break
        }

        return if (mapped.isEmpty()) {
            fallbackSignals(subcategory, region, maxSignals)
        } else {
            mapped
        }
    }

    private fun belongsToSubcategory(keyword: String, subcategory: String): Boolean {
        val lowered = keyword.lowercase()
        val hints = when (subcategory.uppercase()) {
            "ECONOMY" -> listOf("금리", "환율", "주가", "증시", "비트코인", "물가", "코스피", "나스닥", "inflation", "stock", "bitcoin")
            "TECH" -> listOf("ai", "openai", "apple", "google", "meta", "테슬라", "반도체", "칩", "갤럭시", "아이폰")
            "SPORTS" -> listOf("축구", "야구", "농구", "월드컵", "올림픽", "premier", "nba", "mlb", "epl", "경기")
            "POLITICS" -> listOf("선거", "대통령", "의회", "정책", "관세", "외교", "election", "policy", "tariff")
            "CULTURE" -> listOf("영화", "드라마", "음악", "넷플릭스", "k-pop", "bts", "공연", "festival")
            "CRYPTO" -> listOf("비트코인", "이더리움", "리플", "솔라나", "알트코인", "코인", "bitcoin", "ethereum", "crypto", "defi")
            else -> emptyList()
        }

        if (hints.isEmpty()) return true
        return hints.any { lowered.contains(it) }
    }

    private fun fallbackSignals(subcategory: String, region: String, maxSignals: Int): List<TrendSignalDto> {
        val fallbackKeywords = when (subcategory.uppercase()) {
            "ECONOMY" -> listOf("미국 금리", "비트코인", "나스닥", "원달러 환율", "물가 지표")
            "TECH" -> listOf("OpenAI", "애플", "테슬라", "엔비디아", "AI 반도체")
            "SPORTS" -> listOf("프리미어리그", "챔피언스리그", "MLB", "NBA", "월드컵 예선")
            "POLITICS" -> listOf("미국 대선", "대중 관세", "금리 정책", "국회 표결", "외교 회담")
            "CULTURE" -> listOf("넷플릭스 신작", "K-POP 컴백", "박스오피스", "음원 차트", "시상식")
            "CRYPTO" -> listOf("비트코인", "이더리움", "솔라나", "SEC 규제", "암호화폐 ETF")
            else -> listOf("시장 심리", "경제 이슈", "정책 발표", "주요 뉴스", "글로벌 이벤트")
        }

        return fallbackKeywords.take(maxSignals).mapIndexed { index, keyword ->
            TrendSignalDto(
                subcategory = subcategory,
                keyword = keyword,
                trendScore = (100 - index * 10).coerceAtLeast(10),
                region = region,
                source = "FALLBACK"
            )
        }
    }

    private fun JsonNode.firstOrNull(): JsonNode? {
        return if (this.isArray && this.size() > 0) this[0] else null
    }
}
