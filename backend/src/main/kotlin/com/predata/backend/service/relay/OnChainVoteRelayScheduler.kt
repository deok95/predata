package com.predata.backend.service.relay

import com.predata.backend.config.properties.RelayProperties
import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.OnChainVoteRelay
import com.predata.backend.repository.OnChainVoteRelayRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 온체인 투표 릴레이 스케줄러
 *
 * 5초 주기로 PENDING 배치와 FAILED 재시도 배치를 처리한다.
 * RelayProperties.enabled=false 이면 전체 비활성.
 *
 * 상태 전이:
 *   PENDING  → SUBMITTED → CONFIRMED (성공)
 *                        → FAILED    (RetryableFailure, retryCount < maxRetry)
 *                        → FAILED_FINAL (FinalFailure 또는 retryCount >= maxRetry)
 *   FAILED   → PENDING   재진입 (nextRetryAt 경과 후 배치에 포함)
 *
 * 백오프: min(30 * 2^(retryCount-1), 3600) 초  → 1st: 30s, 2nd: 60s, 3rd: 120s ...
 */
@Component
class OnChainVoteRelayScheduler(
    private val relayProcessor: RelayProcessor,
    private val relayProperties: RelayProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5000)
    fun run() {
        if (!relayProperties.enabled) return

        try {
            relayProcessor.processBatch()
        } catch (e: Exception) {
            logger.error("Relay scheduler batch failed", e)
        }
    }
}

/**
 * 릴레이 배치 처리기 (트랜잭션 경계 분리)
 *
 * 주의: 현재 구현은 단일 트랜잭션 내에서 relay()를 호출한다.
 * MockOnChainRelayService는 즉시 반환하므로 문제 없음.
 * 실제 Polygon 구현 시에는 claimBatch / applyResult를 별도 트랜잭션으로 분리할 것.
 */
@Service
class RelayProcessor(
    private val onChainVoteRelayRepository: OnChainVoteRelayRepository,
    private val relayService: OnChainRelayService,
    private val relayProperties: RelayProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processBatch() {
        val now = LocalDateTime.now(ZoneOffset.UTC)

        val pending = onChainVoteRelayRepository.findByStatusForUpdateSkipLocked(
            OnChainRelayStatus.PENDING.name,
            relayProperties.batchSize,
        )
        pending.forEach { process(it, now) }

        val retryable = onChainVoteRelayRepository.findRetryableForUpdateSkipLocked(
            now,
            relayProperties.maxRetry,
            relayProperties.batchSize,
        )
        retryable.forEach { process(it, now) }
    }

    private fun process(relay: OnChainVoteRelay, batchTime: LocalDateTime) {
        relay.status = OnChainRelayStatus.SUBMITTED
        relay.updatedAt = batchTime

        val result = try {
            relayService.relay(relay)
        } catch (e: Exception) {
            logger.error("Relay service threw exception: voteId={}", relay.voteId, e)
            RelayResult.RetryableFailure(
                reason = "UNEXPECTED_ERROR",
                errorMessage = e.message?.take(500) ?: "unknown error",
            )
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)
        when (result) {
            is RelayResult.Success -> {
                relay.status = OnChainRelayStatus.CONFIRMED
                relay.txHash = result.txHash
                relay.updatedAt = now
                logger.info("Relay confirmed: voteId={} txHash={}", relay.voteId, result.txHash)
            }

            is RelayResult.RetryableFailure -> {
                val newCount = relay.retryCount + 1
                relay.retryCount = newCount
                relay.errorMessage = result.errorMessage.take(500)

                if (newCount >= relayProperties.maxRetry) {
                    relay.status = OnChainRelayStatus.FAILED_FINAL
                    relay.updatedAt = now
                    logger.warn(
                        "Relay exhausted (FAILED_FINAL): voteId={} retries={} reason={}",
                        relay.voteId, newCount, result.reason,
                    )
                } else {
                    relay.status = OnChainRelayStatus.FAILED
                    relay.nextRetryAt = now.plusSeconds(backoffSeconds(newCount - 1))
                    relay.updatedAt = now
                    logger.warn(
                        "Relay retryable: voteId={} retry={}/{} reason={} nextAt={}",
                        relay.voteId, newCount, relayProperties.maxRetry, result.reason, relay.nextRetryAt,
                    )
                }
            }

            is RelayResult.FinalFailure -> {
                relay.status = OnChainRelayStatus.FAILED_FINAL
                relay.errorMessage = result.errorMessage.take(500)
                relay.updatedAt = now
                logger.error(
                    "Relay final failure: voteId={} reason={}",
                    relay.voteId, result.reason,
                )
            }
        }

        onChainVoteRelayRepository.save(relay)
    }

    /** 지수 백오프: min(30 * 2^(newCount-1), 3600) 초. newCount-1 을 인자로 받는다. */
    private fun backoffSeconds(exponent: Int): Long =
        minOf(30L * (1L shl exponent), 3600L)
}
