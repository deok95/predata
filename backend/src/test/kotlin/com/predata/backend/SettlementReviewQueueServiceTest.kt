package com.predata.backend

import com.predata.backend.domain.SettlementReviewQueue
import com.predata.backend.domain.SettlementReviewReasonCode
import com.predata.backend.domain.SettlementReviewStatus
import com.predata.backend.repository.SettlementReviewQueueRepository
import com.predata.backend.service.SettlementReviewQueueService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

class SettlementReviewQueueServiceTest {

    private val repository: SettlementReviewQueueRepository = mock()
    private val service = SettlementReviewQueueService(repository)

    private fun entry(
        id: Long = 1L,
        questionId: Long = 100L,
        status: SettlementReviewStatus = SettlementReviewStatus.PENDING_RETRY,
        retryCount: Int = 0,
        maxRetry: Int = 3,
    ): SettlementReviewQueue = SettlementReviewQueue(
        id = id,
        questionId = questionId,
        status = status,
        reasonCode = SettlementReviewReasonCode.SOURCE_UNAVAILABLE,
        retryCount = retryCount,
        maxRetry = maxRetry,
    )

    @BeforeEach
    fun setUp() {
        Mockito.doAnswer { invocation -> invocation.getArgument<SettlementReviewQueue>(0) }
            .`when`(repository)
            .save(ArgumentMatchers.any(SettlementReviewQueue::class.java))
    }

    // ─── enqueueOrUpdate ─────────────────────────────────────────────────────

    @Test
    fun `enqueueOrUpdate - 신규 등록`() {
        whenever(repository.findByQuestionId(100L)).thenReturn(null)

        service.enqueueOrUpdate(100L, SettlementReviewReasonCode.SOURCE_UNAVAILABLE, "소스 없음")

        val captor = argumentCaptor<SettlementReviewQueue>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.questionId).isEqualTo(100L)
        assertThat(saved.status).isEqualTo(SettlementReviewStatus.PENDING_RETRY)
        assertThat(saved.reasonCode).isEqualTo(SettlementReviewReasonCode.SOURCE_UNAVAILABLE)
        assertThat(saved.nextRetryAt).isNotNull()  // 5분 뒤 예약
    }

    @Test
    fun `enqueueOrUpdate - 이미 존재하면 reasonCode만 갱신`() {
        val existing = entry(status = SettlementReviewStatus.PENDING_RETRY)
        whenever(repository.findByQuestionId(100L)).thenReturn(existing)

        service.enqueueOrUpdate(100L, SettlementReviewReasonCode.EXCEPTION, "예외 발생")

        verify(repository).save(existing)
        assertThat(existing.reasonCode).isEqualTo(SettlementReviewReasonCode.EXCEPTION)
    }

    @Test
    fun `enqueueOrUpdate - RESOLVED 항목은 재등록 무시`() {
        val resolved = entry(status = SettlementReviewStatus.RESOLVED)
        whenever(repository.findByQuestionId(100L)).thenReturn(resolved)

        val result = service.enqueueOrUpdate(100L, SettlementReviewReasonCode.EXCEPTION)

        verify(repository, never()).save(any())
        assertThat(result.status).isEqualTo(SettlementReviewStatus.RESOLVED)
    }

    // ─── markRetryFailed ─────────────────────────────────────────────────────

    @Test
    fun `markRetryFailed - 한도 미만이면 retryCount 증가 후 nextRetryAt 예약`() {
        val entry = entry(retryCount = 0, maxRetry = 3)

        service.markRetryFailed(entry, "오류 메시지")

        verify(repository).save(entry)
        assertThat(entry.retryCount).isEqualTo(1)
        assertThat(entry.status).isEqualTo(SettlementReviewStatus.PENDING_RETRY)
        assertThat(entry.nextRetryAt).isNotNull()
    }

    @Test
    fun `markRetryFailed - 한도 도달 시 NEEDS_MANUAL 전환`() {
        val entry = entry(retryCount = 2, maxRetry = 3)

        service.markRetryFailed(entry, "마지막 실패")

        verify(repository).save(entry)
        assertThat(entry.retryCount).isEqualTo(3)
        assertThat(entry.status).isEqualTo(SettlementReviewStatus.NEEDS_MANUAL)
    }

    // ─── markResolved ─────────────────────────────────────────────────────────

    @Test
    fun `markResolved - 존재하는 항목 RESOLVED 처리`() {
        val entry = entry()
        whenever(repository.findByQuestionId(100L)).thenReturn(entry)

        service.markResolved(100L)

        verify(repository).save(entry)
        assertThat(entry.status).isEqualTo(SettlementReviewStatus.RESOLVED)
    }

    @Test
    fun `markResolved - 존재하지 않으면 아무 것도 하지 않음`() {
        whenever(repository.findByQuestionId(999L)).thenReturn(null)

        service.markResolved(999L)

        verify(repository, never()).save(any())
    }
}
