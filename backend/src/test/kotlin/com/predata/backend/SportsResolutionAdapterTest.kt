package com.predata.backend

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.service.MatchResult
import com.predata.backend.service.SportsApiService
import com.predata.backend.service.settlement.adapters.SportsResolutionAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class SportsResolutionAdapterTest {

    private val sportsApiService: SportsApiService = mock()
    private val adapter = SportsResolutionAdapter(sportsApiService)

    private fun baseQuestion(
        resolutionSource: String?,
        resolutionRule: String,
    ): Question = Question(
        title = "스포츠 테스트",
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
    fun `supportsSource - sports 스킴`() {
        assertThat(adapter.supportsSource("sports://12345")).isTrue()
    }

    @Test
    fun `supportsSource - match 스킴`() {
        assertThat(adapter.supportsSource("match://9999")).isTrue()
    }

    @Test
    fun `supportsSource - 순수 숫자 (하위 호환)`() {
        assertThat(adapter.supportsSource("12345")).isTrue()
    }

    @Test
    fun `supportsSource - stock 스킴 거부`() {
        assertThat(adapter.supportsSource("stock://AAPL")).isFalse()
    }

    @Test
    fun `supportsSource - null 거부`() {
        assertThat(adapter.supportsSource(null)).isFalse()
    }

    // ─── source 파싱 실패 → 보류 ─────────────────────────────────────────────

    @Test
    fun `resolutionSource null → 보류`() {
        val result = adapter.resolve(baseQuestion(null, "HOME_WIN"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    @Test
    fun `인식 불가 source 형식 → 보류`() {
        val result = adapter.resolve(baseQuestion("unknown://abc", "HOME_WIN"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── rule 파싱 실패 → 보류 ───────────────────────────────────────────────

    @Test
    fun `잘못된 resolutionRule → 보류`() {
        val result = adapter.resolve(baseQuestion("sports://123", "홈팀 승리"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── 경기 데이터 없음 → 보류 ─────────────────────────────────────────────

    @Test
    fun `API 응답 없음 → 보류`() {
        whenever(sportsApiService.fetchMatchResult("123")).thenReturn(null)
        val result = adapter.resolve(baseQuestion("sports://123", "HOME_WIN"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── 경기 미종료 → 보류 ──────────────────────────────────────────────────

    @Test
    fun `경기 LIVE 상태 → 보류`() {
        whenever(sportsApiService.fetchMatchResult("456")).thenReturn(
            MatchResult(homeScore = 1, awayScore = 0, result = "HOME_WIN", status = "LIVE")
        )
        val result = adapter.resolve(baseQuestion("match://456", "HOME_WIN"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    @Test
    fun `경기 SCHEDULED 상태 → 보류`() {
        whenever(sportsApiService.fetchMatchResult("789")).thenReturn(
            MatchResult(homeScore = 0, awayScore = 0, result = "DRAW", status = "SCHEDULED")
        )
        val result = adapter.resolve(baseQuestion("789", "DRAW"))
        assertThat(result.result).isNull()
        assertThat(result.confidence).isEqualTo(0.0)
    }

    // ─── 정상 판정 (경기 종료) ───────────────────────────────────────────────

    @Test
    fun `HOME_WIN 예측 - 실제 HOME_WIN → YES`() {
        whenever(sportsApiService.fetchMatchResult("100")).thenReturn(
            MatchResult(homeScore = 2, awayScore = 1, result = "HOME_WIN", status = "FINISHED")
        )
        val result = adapter.resolve(baseQuestion("sports://100", "HOME_WIN"))
        assertThat(result.result).isEqualTo(FinalResult.YES)
        assertThat(result.confidence).isEqualTo(1.0)
        assertThat(result.sourceUrl).contains("100")
        assertThat(result.sourcePayload).contains("FINISHED")
    }

    @Test
    fun `HOME_WIN 예측 - 실제 AWAY_WIN → NO`() {
        whenever(sportsApiService.fetchMatchResult("101")).thenReturn(
            MatchResult(homeScore = 0, awayScore = 3, result = "AWAY_WIN", status = "FINISHED")
        )
        val result = adapter.resolve(baseQuestion("match://101", "HOME_WIN"))
        assertThat(result.result).isEqualTo(FinalResult.NO)
        assertThat(result.confidence).isEqualTo(1.0)
    }

    @Test
    fun `AWAY_WIN 예측 - 실제 AWAY_WIN → YES`() {
        whenever(sportsApiService.fetchMatchResult("200")).thenReturn(
            MatchResult(homeScore = 1, awayScore = 2, result = "AWAY_WIN", status = "FINISHED")
        )
        val result = adapter.resolve(baseQuestion("sports://200", "AWAY_WIN"))
        assertThat(result.result).isEqualTo(FinalResult.YES)
    }

    @Test
    fun `DRAW 예측 - 실제 DRAW → YES`() {
        whenever(sportsApiService.fetchMatchResult("300")).thenReturn(
            MatchResult(homeScore = 1, awayScore = 1, result = "DRAW", status = "FINISHED")
        )
        val result = adapter.resolve(baseQuestion("300", "DRAW"))
        assertThat(result.result).isEqualTo(FinalResult.YES)
    }

    @Test
    fun `rule 대소문자 무시 - home_win → HOME_WIN 판정`() {
        whenever(sportsApiService.fetchMatchResult("400")).thenReturn(
            MatchResult(homeScore = 3, awayScore = 0, result = "HOME_WIN", status = "FINISHED")
        )
        val result = adapter.resolve(baseQuestion("sports://400", "home_win"))
        assertThat(result.result).isEqualTo(FinalResult.YES)
    }
}
