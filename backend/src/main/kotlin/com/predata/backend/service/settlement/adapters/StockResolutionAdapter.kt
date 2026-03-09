package com.predata.backend.service.settlement.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import com.predata.backend.service.settlement.StockPriceClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 주가 기반 정산 어댑터.
 *
 * resolutionSource 형식: stock://AAPL
 * resolutionRule 형식:   CLOSE >= 200  |  PRICE < 150.5
 *   - CLOSE: 전일 종가(chartPreviousClose) 기준
 *   - PRICE: 실시간 시장가(regularMarketPrice) 기준
 *   - 지원 연산자: >=  <=  >  <  ==
 *
 * 데이터 미확정/파싱 실패 시: result=null, confidence=0.0 → 자동정산 보류
 */
@Component
class StockResolutionAdapter(
    private val stockPriceClient: StockPriceClient,
) : ResolutionAdapter {

    private val log = LoggerFactory.getLogger(StockResolutionAdapter::class.java)
    private val objectMapper = ObjectMapper()

    private val STOCK_SCHEME = "stock://"
    private val RULE_PATTERN = Regex("""^(CLOSE|PRICE)\s*(>=|<=|>|<|==)\s*(\d+(?:\.\d+)?)$""")

    override fun supports(marketType: MarketType): Boolean = marketType == MarketType.VERIFIABLE

    override fun supportsSource(resolutionSource: String?): Boolean =
        resolutionSource?.startsWith(STOCK_SCHEME) == true

    override fun resolve(question: Question): ResolutionResult {
        val source = question.resolutionSource
            ?: return pendingResult("resolutionSource is null")

        val ticker = parseTicker(source)
            ?: return pendingResult("ticker parse failed: $source")

        val rule = parseRule(question.resolutionRule)
            ?: return pendingResult("resolutionRule parse failed: '${question.resolutionRule}'")

        val quote = stockPriceClient.fetchQuote(ticker)
            ?: return pendingResult("stock price unavailable for $ticker")

        val actualPrice = when (rule.metric) {
            "CLOSE" -> quote.previousClose
            "PRICE" -> quote.price
            else    -> return pendingResult("unknown metric: ${rule.metric}")
        }

        val conditionMet = evaluate(actualPrice, rule.operator, rule.threshold)
        val result = if (conditionMet) FinalResult.YES else FinalResult.NO

        log.info(
            "[StockAdapter] {} {} {} {} (actual={}) → {}",
            ticker, rule.metric, rule.operator, rule.threshold, actualPrice, result,
        )

        val payload = buildPayload(ticker, rule, actualPrice, conditionMet, result)
        return ResolutionResult(
            result = result,
            sourcePayload = payload,
            sourceUrl = quote.sourceUrl,
            confidence = 1.0,
        )
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private fun parseTicker(source: String): String? {
        val ticker = source.removePrefix(STOCK_SCHEME).trim()
        return ticker.ifBlank { null }
    }

    private data class ParsedRule(val metric: String, val operator: String, val threshold: Double)

    private fun parseRule(rule: String): ParsedRule? {
        val match = RULE_PATTERN.find(rule.trim()) ?: return null
        val metric    = match.groupValues[1]
        val operator  = match.groupValues[2]
        val threshold = match.groupValues[3].toDoubleOrNull() ?: return null
        return ParsedRule(metric, operator, threshold)
    }

    private fun evaluate(actual: Double, operator: String, threshold: Double): Boolean = when (operator) {
        ">=" -> actual >= threshold
        "<=" -> actual <= threshold
        ">"  -> actual >  threshold
        "<"  -> actual <  threshold
        "==" -> actual == threshold
        else -> false
    }

    private fun buildPayload(
        ticker: String,
        rule: ParsedRule,
        actualPrice: Double,
        conditionMet: Boolean,
        result: FinalResult,
    ): String = objectMapper.writeValueAsString(
        mapOf(
            "ticker"       to ticker,
            "metric"       to rule.metric,
            "operator"     to rule.operator,
            "threshold"    to rule.threshold,
            "actualPrice"  to actualPrice,
            "conditionMet" to conditionMet,
            "result"       to result.name,
        )
    )

    private fun pendingResult(reason: String): ResolutionResult {
        log.info("[StockAdapter] 보류: {}", reason)
        return ResolutionResult(result = null, sourcePayload = null, sourceUrl = null, confidence = 0.0)
    }
}
