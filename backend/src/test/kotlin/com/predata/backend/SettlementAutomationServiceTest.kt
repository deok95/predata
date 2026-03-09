package com.predata.backend

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VotingPhase
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.AuditService
import com.predata.backend.service.SettlementAutomationService
import com.predata.backend.service.SettlementService
import com.predata.backend.service.settlement.adapters.ResolutionAdapterRegistry
import com.predata.backend.service.settlement.adapters.ResolutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class SettlementAutomationServiceTest {

    private val questionRepository: QuestionRepository = mock()
    private val adapterRegistry: ResolutionAdapterRegistry = mock()
    private val settlementService: SettlementService = mock()
    private val auditService: AuditService = mock()

    private val service = SettlementAutomationService(
        questionRepository, adapterRegistry, settlementService, auditService,
    )

    // questionId는 서비스 메서드 파라미터로만 사용되므로 Question.id 세팅 불필요
    private fun verifiableQuestion(): Question = Question(
        title = "VERIFIABLE 정산 테스트",
        status = QuestionStatus.BETTING,
        marketType = MarketType.VERIFIABLE,
        resolutionSource = "sports://999",
        resolutionRule = "HOME_WIN",
        voteResultSettlement = false,
        votingEndAt = LocalDateTime.now().minusDays(2),
        bettingStartAt = LocalDateTime.now().minusDays(1),
        bettingEndAt = LocalDateTime.now().plusDays(1),
        expiredAt = LocalDateTime.now().plusDays(2),
    )

    private fun voteResultQuestion(phase: VotingPhase = VotingPhase.VOTING_REVEAL_CLOSED): Question = Question(
        title = "VOTE_RESULT 정산 테스트",
        status = QuestionStatus.BETTING,
        marketType = MarketType.OPINION,
        resolutionRule = "기본",
        voteResultSettlement = true,
        votingPhase = phase,
        votingEndAt = LocalDateTime.now().minusDays(2),
        bettingStartAt = LocalDateTime.now().minusDays(1),
        bettingEndAt = LocalDateTime.now().plusDays(1),
        expiredAt = LocalDateTime.now().plusDays(2),
    )

    // ─── confidence 기준 통과 → 정산 진행 ────────────────────────────────────

    @Test
    fun `confidence 1_0 + result 확정 + sourceUrl 존재 → 정산 진행`() {
        val q = verifiableQuestion()
        whenever(questionRepository.findByIdWithLock(1L)).thenReturn(q)
        whenever(adapterRegistry.resolve(q)).thenReturn(
            ResolutionResult(FinalResult.YES, """{"status":"FINISHED"}""", "https://example.com", 1.0)
        )
        whenever(settlementService.initiateSettlement(any(), any(), any())).thenReturn(mock())
        whenever(settlementService.finalizeSettlement(any(), any())).thenReturn(mock())

        val result = service.autoSettleWithVerification(1L)

        assertThat(result).isNotNull()
        verify(settlementService).initiateSettlement(1L, FinalResult.YES, "https://example.com")
    }

    // ─── confidence 부족 → 보류 ───────────────────────────────────────────────

    @Test
    fun `confidence 0_95 (0_99 미만) → 정산 보류`() {
        val q = verifiableQuestion()
        whenever(questionRepository.findByIdWithLock(1L)).thenReturn(q)
        whenever(adapterRegistry.resolve(q)).thenReturn(
            ResolutionResult(FinalResult.YES, """{"status":"LIVE"}""", "https://example.com", 0.95)
        )

        val result = service.autoSettleWithVerification(1L)

        assertThat(result).isNull()
        verify(settlementService, never()).initiateSettlement(any(), any(), any())
    }

    // ─── result null → 보류 ──────────────────────────────────────────────────

    @Test
    fun `result null → 정산 보류`() {
        val q = verifiableQuestion()
        whenever(questionRepository.findByIdWithLock(1L)).thenReturn(q)
        whenever(adapterRegistry.resolve(q)).thenReturn(
            ResolutionResult(null, null, null, 0.0)
        )

        val result = service.autoSettleWithVerification(1L)

        assertThat(result).isNull()
        verify(settlementService, never()).initiateSettlement(any(), any(), any())
    }

    // ─── VERIFIABLE sourceUrl null → 보류 ────────────────────────────────────

    @Test
    fun `VERIFIABLE 질문 sourceUrl null → 정산 보류`() {
        val q = verifiableQuestion()
        whenever(questionRepository.findByIdWithLock(1L)).thenReturn(q)
        whenever(adapterRegistry.resolve(q)).thenReturn(
            ResolutionResult(FinalResult.YES, """{"status":"FINISHED"}""", sourceUrl = null, confidence = 1.0)
        )

        val result = service.autoSettleWithVerification(1L)

        assertThat(result).isNull()
        verify(settlementService, never()).initiateSettlement(any(), any(), any())
    }

    // ─── VOTE_RESULT reveal phase 체크 ───────────────────────────────────────

    @Test
    fun `VOTE_RESULT reveal 미완료(REVEAL_OPEN) → 어댑터 호출 없이 보류`() {
        val q = voteResultQuestion(phase = VotingPhase.VOTING_REVEAL_OPEN)
        whenever(questionRepository.findByIdWithLock(2L)).thenReturn(q)

        val result = service.autoSettleWithVerification(2L)

        assertThat(result).isNull()
        verify(adapterRegistry, never()).resolve(any())
    }

    @Test
    fun `VOTE_RESULT REVEAL_CLOSED + confidence 충족 → 정산 진행 (sourceUrl null 허용)`() {
        val q = voteResultQuestion(phase = VotingPhase.VOTING_REVEAL_CLOSED)
        whenever(questionRepository.findByIdWithLock(2L)).thenReturn(q)
        whenever(adapterRegistry.resolve(q)).thenReturn(
            ResolutionResult(FinalResult.YES, """{"yesVotes":10}""", sourceUrl = null, confidence = 1.0)
        )
        whenever(settlementService.initiateSettlement(any(), any(), any())).thenReturn(mock())
        whenever(settlementService.finalizeSettlement(any(), any())).thenReturn(mock())

        val result = service.autoSettleWithVerification(2L)

        assertThat(result).isNotNull()
        verify(settlementService).initiateSettlement(2L, FinalResult.YES, null)
    }

    // ─── 어댑터 예외 → null 반환 (장애 격리) ─────────────────────────────────

    @Test
    fun `어댑터 예외 발생 → null 반환 (예외 전파 없음)`() {
        val q = verifiableQuestion()
        whenever(questionRepository.findByIdWithLock(1L)).thenReturn(q)
        whenever(adapterRegistry.resolve(q)).thenThrow(RuntimeException("API timeout"))

        val result = service.autoSettleWithVerification(1L)

        assertThat(result).isNull()
        verify(settlementService, never()).initiateSettlement(any(), any(), any())
    }
}
