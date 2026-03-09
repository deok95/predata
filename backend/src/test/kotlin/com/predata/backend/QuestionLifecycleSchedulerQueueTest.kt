package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.service.*
import com.predata.backend.service.amm.SwapService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * QuestionLifecycleScheduler가 transitionBettingToSettled() 처리 중
 * 자동정산 실패/보류 시 SettlementReviewQueueService.enqueueOrUpdate를 호출하는지 검증.
 */
class QuestionLifecycleSchedulerQueueTest {

    private val questionRepository: QuestionRepository = mock()
    private val activityRepository: ActivityRepository = mock()
    private val settlementService: SettlementService = mock()
    private val settlementAutomationService: SettlementAutomationService = mock()
    private val reviewQueueService: SettlementReviewQueueService = mock()
    private val marketPoolRepository: MarketPoolRepository = mock()
    private val swapService: SwapService = mock()

    private val scheduler = QuestionLifecycleScheduler(
        questionRepository = questionRepository,
        activityRepository = activityRepository,
        settlementService = settlementService,
        settlementAutomationService = settlementAutomationService,
        reviewQueueService = reviewQueueService,
        marketPoolRepository = marketPoolRepository,
        swapService = swapService,
        apiKey = "",
    )

    private fun bettingExpiredQuestion(
        id: Long = 1L,
        marketType: MarketType = MarketType.VERIFIABLE,
        voteResultSettlement: Boolean = false,
    ): Question = Question(
        id = id,
        title = "테스트 질문",
        status = QuestionStatus.BETTING,
        marketType = marketType,
        resolutionRule = "HOME_WIN",
        voteResultSettlement = voteResultSettlement,
        votingEndAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
        bettingStartAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
        bettingEndAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10),
        expiredAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(1),
    )

    // ─── autoSettle null 반환 → VERIFIABLE 질문은 큐 등록 ───────────────────

    @Test
    fun `VERIFIABLE 질문 autoSettle null 반환 시 SOURCE_UNAVAILABLE로 큐 등록`() {
        val q = bettingExpiredQuestion(id = 1L, marketType = MarketType.VERIFIABLE)
        whenever(questionRepository.findBettingExpiredBefore(any())).thenReturn(listOf(q))
        whenever(questionRepository.findByIdWithLock(1L)).thenReturn(q)
        whenever(settlementAutomationService.autoSettleWithVerification(1L)).thenReturn(null)

        scheduler.transitionBettingToSettled()

        verify(reviewQueueService).enqueueOrUpdate(eq(1L), eq(SettlementReviewReasonCode.SOURCE_UNAVAILABLE), anyOrNull())
    }

    // ─── autoSettle 예외 → EXCEPTION으로 큐 등록 ────────────────────────────

    @Test
    fun `VERIFIABLE 질문 autoSettle 예외 발생 시 EXCEPTION으로 큐 등록`() {
        val q = bettingExpiredQuestion(id = 2L, marketType = MarketType.VERIFIABLE)
        whenever(questionRepository.findBettingExpiredBefore(any())).thenReturn(listOf(q))
        whenever(questionRepository.findByIdWithLock(2L)).thenReturn(q)
        whenever(settlementAutomationService.autoSettleWithVerification(2L))
            .thenThrow(RuntimeException("데이터 소스 타임아웃"))

        scheduler.transitionBettingToSettled()

        verify(reviewQueueService).enqueueOrUpdate(eq(2L), eq(SettlementReviewReasonCode.EXCEPTION), anyOrNull())
    }

    // ─── OPINION 질문(수동정산 대상)은 큐 등록 안 함 ────────────────────────

    @Test
    fun `OPINION 질문 autoSettle null 반환 시 큐 등록 안 함`() {
        val q = bettingExpiredQuestion(id = 3L, marketType = MarketType.OPINION, voteResultSettlement = false)
        whenever(questionRepository.findBettingExpiredBefore(any())).thenReturn(listOf(q))
        whenever(questionRepository.findByIdWithLock(3L)).thenReturn(q)
        whenever(settlementAutomationService.autoSettleWithVerification(3L)).thenReturn(null)

        scheduler.transitionBettingToSettled()

        verify(reviewQueueService, never()).enqueueOrUpdate(any(), any(), anyOrNull())
    }

    // ─── VOTE_RESULT reveal 미완료 → 큐 등록 안 함 ─────────────────────────

    @Test
    fun `VOTE_RESULT reveal 미완료(REVEAL_OPEN)이면 큐 등록 안 함`() {
        val base = bettingExpiredQuestion(id = 4L, marketType = MarketType.OPINION, voteResultSettlement = true)
        val q = base.copy(votingPhase = VotingPhase.VOTING_REVEAL_OPEN)
        whenever(questionRepository.findBettingExpiredBefore(any())).thenReturn(listOf(q))
        whenever(questionRepository.findByIdWithLock(4L)).thenReturn(q)
        whenever(settlementAutomationService.autoSettleWithVerification(4L)).thenReturn(null)

        scheduler.transitionBettingToSettled()

        verify(reviewQueueService, never()).enqueueOrUpdate(any(), any(), anyOrNull())
    }
}
