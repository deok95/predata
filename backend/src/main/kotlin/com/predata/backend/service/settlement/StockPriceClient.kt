package com.predata.backend.service.settlement

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.config.properties.StockProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Yahoo Finance API를 통해 주가 데이터를 조회하는 클라이언트.
 * 무료 엔드포인트 사용 (별도 API 키 불필요).
 *
 * resolutionSource 형식: stock://AAPL
 * 반환 price: regularMarketPrice (실시간/장중 기준)
 * 반환 previousClose: chartPreviousClose (전일 종가)
 */
@Component
class StockPriceClient(
    private val props: StockProperties,
) {
    private val log = LoggerFactory.getLogger(StockPriceClient::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    data class StockQuote(
        val ticker: String,
        val price: Double,
        val previousClose: Double,
        val sourceUrl: String,
        val rawPayload: String,
    )

    fun fetchQuote(ticker: String): StockQuote? {
        if (!props.enabled) {
            log.info("[StockPrice] API disabled, skipping fetch for {}", ticker)
            return null
        }
        val url = "${props.yahooBaseUrl}/v8/finance/chart/$ticker?interval=1d&range=1d"
        return try {
            val response = restTemplate.getForObject(url, String::class.java) ?: return null
            parseQuote(ticker, response, url)
        } catch (e: Exception) {
            log.warn("[StockPrice] Failed to fetch quote for {}: {}", ticker, e.message)
            null
        }
    }

    private fun parseQuote(ticker: String, json: String, sourceUrl: String): StockQuote? {
        return try {
            val root = objectMapper.readTree(json)
            val meta = root.path("chart").path("result").get(0)?.path("meta")
                ?: run { log.warn("[StockPrice] No chart result for {}", ticker); return null }

            val price = meta.path("regularMarketPrice").asDouble(Double.NaN)
            val previousClose = meta.path("chartPreviousClose").asDouble(Double.NaN)

            if (price.isNaN()) {
                log.warn("[StockPrice] regularMarketPrice missing for {}", ticker)
                return null
            }
            StockQuote(
                ticker = ticker,
                price = price,
                previousClose = if (previousClose.isNaN()) price else previousClose,
                sourceUrl = sourceUrl,
                rawPayload = json,
            )
        } catch (e: Exception) {
            log.warn("[StockPrice] Failed to parse quote for {}: {}", ticker, e.message)
            null
        }
    }
}
