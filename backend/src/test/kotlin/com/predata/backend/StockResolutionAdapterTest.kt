package com.predata.backend

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.service.settlement.StockPriceClient
import com.predata.backend.service.settlement.adapters.StockResolutionAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class StockResolutionAdapterTest {

    private val stockPriceClient: StockPriceClient = mock()
    private val adapter = StockResolutionAdapter(stockPriceClient)

    private fun baseQuestion(
        resolutionSource: String?,
        resolutionRule: String,
    ): Question = Question(
        title = "주가 테스트",
        status = QuestionStatus.BETTING,
        marketType = MarketType.VERIFIABLE,
        resolutionSource = resolutionSource,
        resolutionRule = resolutionRule,
        votingEndAt = LocalDateTime.now().minusDays(2),
        bettingStartAt = LocalDateTime.now().minusDays(1),
        bettingEndAt = LocalDateTime.now().plusDays(1),
        expiredAt = LocalDateTime.now().plusDays(2),
    )

    // ─── supportsSource ───────────────────────────────────────────────────────

    @Test
    fun `supportsSource - stock 스킴 인식`() {
        assertThat(adapter.supportsSource("stock://AAPL")).isTrue()
        assertThat(adapter.supportsSource("stock://005930.KS")).isTrue()
    }

    @Test
    fun `supportsSource - 다른 스킴 거부`() {
        assertThat(adapter.supportsSource("sports://123")).isFalse()
        assertThat(adapter.supportsSource("match://123")).isFalse()
        assertThat(adapter.supportsSource("12345")).isFalse()
        assertThat(adapter.supportsSource(null)).isFalse()
    }

    // ─── source 파싱 실패 → 보류 ─────────────────────────────────────────────

    @Test
    fun `resolutionSource null → 보류`() {
        val result = adapter.resolve(baseQuestion(null, "CLOSE >= 200"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    @Test
    fun `resolutionSource 빈 티커 → 보류`() {
        val result = adapter.resolve(baseQuestion("stock://", "CLOSE >= 200"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── rule 파싱 실패 → 보류 ───────────────────────────────────────────────

    @Test
    fun `잘못된 resolutionRule → 보류`() {
        val result = adapter.resolve(baseQuestion("stock://AAPL", "애플 주가 200 이상?"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── 가격 조회 실패 → 보류 ───────────────────────────────────────────────

    @Test
    fun `가격 데이터 없음(API 실패) → 보류`() {
        whenever(stockPriceClient.fetchQuote("AAPL")).thenReturn(null)
        val result = adapter.resolve(baseQuestion("stock://AAPL", "CLOSE >= 200"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── 정상 판정 ────────────────────────────────────────────────────────────

    @Test
    fun `CLOSE 200 이상 충족 - YES`() {
        whenever(stockPriceClient.fetchQuote("AAPL")).thenReturn(quote("AAPL", price = 210.0, prevClose = 205.0))
        val result = adapter.resolve(baseQuestion("stock://AAPL", "CLOSE >= 200"))
        assertThat(result.result).isEqualTo(FinalResult.YES)
        assertThat(result.confidence).isEqualTo(1.0)
        assertThat(result.sourceUrl).isNotNull()
    }

    @Test
    fun `CLOSE 200 이상 미충족 - NO`() {
        whenever(stockPriceClient.fetchQuote("AAPL")).thenReturn(quote("AAPL", price = 190.0, prevClose = 190.0))
        val result = adapter.resolve(baseQuestion("stock://AAPL", "CLOSE >= 200"))
        assertThat(result.result).isEqualTo(FinalResult.NO)
        assertThat(result.confidence).isEqualTo(1.0)
    }

    @Test
    fun `PRICE lt 150 충족 → YES`() {
        whenever(stockPriceClient.fetchQuote("TSLA")).thenReturn(quote("TSLA", price = 140.0, prevClose = 200.0))
        val result = adapter.resolve(baseQuestion("stock://TSLA", "PRICE < 150"))
        assertThat(result.result).isEqualTo(FinalResult.YES)
    }

    @Test
    fun `PRICE lt 150 미충족 → NO`() {
        whenever(stockPriceClient.fetchQuote("TSLA")).thenReturn(quote("TSLA", price = 160.0, prevClose = 200.0))
        val result = adapter.resolve(baseQuestion("stock://TSLA", "PRICE < 150"))
        assertThat(result.result).isEqualTo(FinalResult.NO)
    }

    @Test
    fun `지원 연산자 전체 - 경계값 검증`() {
        val testCases = listOf(
            Triple("CLOSE >= 100", 100.0, FinalResult.YES),
            Triple("CLOSE > 100",  100.0, FinalResult.NO),
            Triple("CLOSE <= 100", 100.0, FinalResult.YES),
            Triple("CLOSE < 100",  100.0, FinalResult.NO),
            Triple("CLOSE == 100", 100.0, FinalResult.YES),
        )
        testCases.forEach { (rule, price, expected) ->
            whenever(stockPriceClient.fetchQuote(any())).thenReturn(quote("X", price = 200.0, prevClose = price))
            val actual = adapter.resolve(baseQuestion("stock://X", rule))
            assertThat(actual.result).isEqualTo(expected)
        }
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private fun quote(ticker: String, price: Double, prevClose: Double) = StockPriceClient.StockQuote(
        ticker = ticker,
        price = price,
        previousClose = prevClose,
        sourceUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker",
        rawPayload = """{"chart":{"result":[{"meta":{"regularMarketPrice":$price}}]}}""",
    )
}
