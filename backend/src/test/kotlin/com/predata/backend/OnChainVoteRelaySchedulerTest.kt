package com.predata.backend

import com.predata.backend.domain.Choice
import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.OnChainVoteRelay
import com.predata.backend.repository.OnChainVoteRelayRepository
import com.predata.backend.service.relay.OnChainRelayService
import com.predata.backend.service.relay.RelayProcessor
import com.predata.backend.service.relay.RelayResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

/**
 * VOTE-030: Relay 상태 전이 테스트
 *
 * 검증 목표:
 * 1. Success -> CONFIRMED, txHash 설정
 * 2. RetryableFailure -> FAILED + backoff + retryCount++
 * 3. maxRetry 도달 -> FAILED_FINAL
 * 4. FinalFailure -> 즉시 FAILED_FINAL
 * 5. backoff 공식: min(30 * 2^retryCount, 3600) 초
 */
@SpringBootTest
@ActiveProfiles("test")
class OnChainVoteRelaySchedulerTest {

    @MockBean
    lateinit var onChainRelayService: OnChainRelayService

    @Autowired
    lateinit var relayProcessor: RelayProcessor

    @Autowired
    lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository

    private val voteIdSeq = AtomicLong(900_000L)
    private val createdIds = mutableListOf<Long>()

    @BeforeEach
    fun setUp() {
        createdIds.clear()
    }

    @AfterEach
    fun tearDown() {
        createdIds.forEach { id ->
            onChainVoteRelayRepository.findById(id).ifPresent { onChainVoteRelayRepository.delete(it) }
        }
    }

    @Test
    fun `Success to CONFIRMED txHash 설정`() {
        val relay = saveRelay(OnChainRelayStatus.PENDING)

        whenever(onChainRelayService.relay(any())).thenReturn(RelayResult.Success(txHash = "0xabc123"))

        relayProcessor.processBatch()

        val updated = onChainVoteRelayRepository.findById(relay.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(OnChainRelayStatus.CONFIRMED)
        assertThat(updated.txHash).isEqualTo("0xabc123")
        assertThat(updated.retryCount).isEqualTo(0)
    }

    @Test
    fun `RetryableFailure to FAILED backoff retryCount 증가`() {
        val relay = saveRelay(OnChainRelayStatus.PENDING)

        whenever(onChainRelayService.relay(any())).thenReturn(
            RelayResult.RetryableFailure(reason = "TIMEOUT", errorMessage = "connection timed out")
        )

        relayProcessor.processBatch()

        val updated = onChainVoteRelayRepository.findById(relay.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(OnChainRelayStatus.FAILED)
        assertThat(updated.retryCount).isEqualTo(1)
        assertThat(updated.errorMessage).contains("connection timed out")
        // backoff: min(30 * 2^0, 3600) = 30s  (첫 실패: newCount=1, backoffSeconds(0) = 30)
        val expectedRetryAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(30)
        assertThat(updated.nextRetryAt).isNotNull()
        assertThat(updated.nextRetryAt!!.toEpochSecond(ZoneOffset.UTC))
            .isBetween(
                expectedRetryAt.toEpochSecond(ZoneOffset.UTC) - 5,
                expectedRetryAt.toEpochSecond(ZoneOffset.UTC) + 5,
            )
    }

    @Test
    fun `RetryableFailure maxRetry 도달 to FAILED_FINAL`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        // maxRetry=8 -> retryCount=7 에서 한 번 더 실패 -> retryCount=8 -> FAILED_FINAL
        val relay = saveRelay(
            status = OnChainRelayStatus.FAILED,
            retryCount = 7,
            nextRetryAt = now.minusSeconds(1),
        )

        whenever(onChainRelayService.relay(any())).thenReturn(
            RelayResult.RetryableFailure(reason = "TIMEOUT", errorMessage = "still failing")
        )

        relayProcessor.processBatch()

        val updated = onChainVoteRelayRepository.findById(relay.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(OnChainRelayStatus.FAILED_FINAL)
        assertThat(updated.retryCount).isEqualTo(8)
    }

    @Test
    fun `FinalFailure to 즉시 FAILED_FINAL`() {
        val relay = saveRelay(OnChainRelayStatus.PENDING)

        whenever(onChainRelayService.relay(any())).thenReturn(
            RelayResult.FinalFailure(reason = "INVALID_DATA", errorMessage = "bad request")
        )

        relayProcessor.processBatch()

        val updated = onChainVoteRelayRepository.findById(relay.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(OnChainRelayStatus.FAILED_FINAL)
        assertThat(updated.retryCount).isEqualTo(0)
        assertThat(updated.errorMessage).contains("bad request")
    }

    @Test
    fun `backoff 최대값 검증 (maxRetry=8 기준)`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        // maxRetry=8 기준 마지막 재시도 전 상태: retryCount=6 -> newCount=7
        // backoffSeconds(6) = min(30 * 2^6, 3600) = min(1920, 3600) = 1920s
        val relay = saveRelay(
            status = OnChainRelayStatus.FAILED,
            retryCount = 6,
            nextRetryAt = now.minusSeconds(1),
        )

        whenever(onChainRelayService.relay(any())).thenReturn(
            RelayResult.RetryableFailure(reason = "TEST", errorMessage = "max backoff test")
        )

        relayProcessor.processBatch()

        val updated = onChainVoteRelayRepository.findById(relay.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(OnChainRelayStatus.FAILED)
        assertThat(updated.retryCount).isEqualTo(7)
        val expectedRetryAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1920)
        assertThat(updated.nextRetryAt!!.toEpochSecond(ZoneOffset.UTC))
            .isBetween(
                expectedRetryAt.toEpochSecond(ZoneOffset.UTC) - 5,
                expectedRetryAt.toEpochSecond(ZoneOffset.UTC) + 5,
            )
    }

    @Test
    fun `FAILED relay nextRetryAt 미경과 시 미처리`() {
        val futureRetry = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10)
        val relay = saveRelay(
            status = OnChainRelayStatus.FAILED,
            retryCount = 1,
            nextRetryAt = futureRetry,
        )

        whenever(onChainRelayService.relay(any())).thenReturn(RelayResult.Success("0xshouldnothappen"))

        relayProcessor.processBatch()

        // nextRetryAt이 미래이므로 배치에 포함되지 않아 상태 그대로
        val updated = onChainVoteRelayRepository.findById(relay.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(OnChainRelayStatus.FAILED)
        assertThat(updated.retryCount).isEqualTo(1)
    }

    private fun saveRelay(
        status: OnChainRelayStatus,
        retryCount: Int = 0,
        nextRetryAt: LocalDateTime? = null,
    ): OnChainVoteRelay {
        val voteId = voteIdSeq.getAndIncrement()
        val relay = onChainVoteRelayRepository.save(
            OnChainVoteRelay(
                voteId = voteId,
                memberId = 99_999L,
                questionId = 99_999L,
                choice = Choice.YES,
                status = status,
                retryCount = retryCount,
                nextRetryAt = nextRetryAt,
            )
        )
        createdIds.add(relay.id!!)
        return relay
    }
}
