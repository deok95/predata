package com.predata.backend

import com.predata.backend.domain.SettlementReviewQueue
import com.predata.backend.domain.SettlementReviewReasonCode
import com.predata.backend.domain.SettlementReviewStatus
import com.predata.backend.service.SettlementAutomationService
import com.predata.backend.service.SettlementResult
import com.predata.backend.service.SettlementReviewQueueService
import com.predata.backend.service.SettlementReviewRetryScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime

class SettlementReviewRetrySchedulerTest {

    private val reviewQueueService: SettlementReviewQueueService = mock()
    private val settlementAutomationService: SettlementAutomationService = mock()
    private val scheduler = SettlementReviewRetryScheduler(reviewQueueService, settlementAutomationService)

    private fun pendingEntry(questionId: Long = 100L): SettlementReviewQueue =
        SettlementReviewQueue(
            id = 1L,
            questionId = questionId,
            status = SettlementReviewStatus.PENDING_RETRY,
            reasonCode = SettlementReviewReasonCode.SOURCE_UNAVAILABLE,
        )

    private fun successResult() = SettlementResult(
        questionId = 100L,
        finalResult = "YES",
        totalBets = 10,
        totalWinners = 5,
        totalPayout = 500L,
        voterRewards = 50L,
        message = "정산 완료",
    )

    @Test
    fun `재시도 성공 시 markResolved 호출`() {
        val entry = pendingEntry()
        whenever(reviewQueueService.findDueForRetry(any())).thenReturn(listOf(entry))
        whenever(settlementAutomationService.autoSettleWithVerification(100L)).thenReturn(successResult())
        whenever(reviewQueueService.findAllByStatus(SettlementReviewStatus.RESOLVED)).thenReturn(listOf(entry))

        scheduler.retryPendingSettlements()

        verify(reviewQueueService).markResolved(100L)
        verify(reviewQueueService, never()).markRetryFailed(any(), any())
    }

    @Test
    fun `autoSettle null 반환 시 markRetryFailed 호출`() {
        val entry = pendingEntry()
        whenever(reviewQueueService.findDueForRetry(any())).thenReturn(listOf(entry))
        whenever(settlementAutomationService.autoSettleWithVerification(100L)).thenReturn(null)
        whenever(reviewQueueService.findAllByStatus(SettlementReviewStatus.RESOLVED)).thenReturn(emptyList())

        scheduler.retryPendingSettlements()

        verify(reviewQueueService).markRetryFailed(eq(entry), any())
        verify(reviewQueueService, never()).markResolved(any())
    }

    @Test
    fun `autoSettle 예외 발생 시 markRetryFailed 호출`() {
        val entry = pendingEntry()
        whenever(reviewQueueService.findDueForRetry(any())).thenReturn(listOf(entry))
        whenever(settlementAutomationService.autoSettleWithVerification(100L))
            .thenThrow(RuntimeException("소스 타임아웃"))
        whenever(reviewQueueService.findAllByStatus(SettlementReviewStatus.RESOLVED)).thenReturn(emptyList())

        scheduler.retryPendingSettlements()

        verify(reviewQueueService).markRetryFailed(eq(entry), argThat { contains("소스 타임아웃") })
        verify(reviewQueueService, never()).markResolved(any())
    }

    @Test
    fun `대기 항목이 없으면 아무 동작 없음`() {
        whenever(reviewQueueService.findDueForRetry(any())).thenReturn(emptyList())

        scheduler.retryPendingSettlements()

        verify(settlementAutomationService, never()).autoSettleWithVerification(any())
        verify(reviewQueueService, never()).markResolved(any())
        verify(reviewQueueService, never()).markRetryFailed(any(), any())
    }
}
