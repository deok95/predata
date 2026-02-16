package com.predata.backend.service

import com.predata.backend.repository.IdempotencyKeyRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 만료된 idempotency key 정리 스케줄러
 * - 테이블 무한 증가를 방지한다.
 */
@Service
class IdempotencyCleanupScheduler(
    private val idempotencyKeyRepository: IdempotencyKeyRepository
) {
    private val logger = LoggerFactory.getLogger(IdempotencyCleanupScheduler::class.java)

    @Scheduled(fixedDelayString = "\${app.voting.idempotency-cleanup-interval-ms:3600000}")
    @Transactional
    fun cleanupExpiredKeys() {
        val deletedCount = idempotencyKeyRepository.deleteExpired(LocalDateTime.now())
        if (deletedCount > 0) {
            logger.info("Expired idempotency keys cleaned: {}", deletedCount)
        }
    }
}
