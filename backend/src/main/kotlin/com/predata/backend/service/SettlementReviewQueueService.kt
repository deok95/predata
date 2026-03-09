package com.predata.backend.service

import com.predata.backend.domain.SettlementReviewQueue
import com.predata.backend.domain.SettlementReviewReasonCode
import com.predata.backend.domain.SettlementReviewStatus
import com.predata.backend.exception.ErrorCode
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.SettlementReviewQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 자동정산 실패/보류 건 관리 서비스.
 *
 * - enqueueOrUpdate: 첫 실패 시 큐 등록, 이미 있으면 reasonCode만 갱신
 * - markRetryFailed: 재시도 실패 기록, 한도 초과 시 NEEDS_MANUAL 자동 전환
 * - markNeedsManual: 즉시 NEEDS_MANUAL 강제 전환
 * - markResolved: 정산 완료 후 RESOLVED 처리
 */
@Service
class SettlementReviewQueueService(
    private val reviewQueueRepository: SettlementReviewQueueRepository,
) {
    private val logger = LoggerFactory.getLogger(SettlementReviewQueueService::class.java)

    companion object {
        private const val DEFAULT_MAX_RETRY = 3
        private val RETRY_DELAY_MINUTES = listOf(5L, 10L, 20L)  // 1차: 5분, 2차: 10분, 3차: 20분
    }

    /**
     * 자동정산 실패/보류 시 큐에 등록한다.
     * question_id UNIQUE 제약으로 이미 존재하면 reasonCode와 detail을 갱신한다.
     */
    @Transactional
    fun enqueueOrUpdate(
        questionId: Long,
        reasonCode: SettlementReviewReasonCode,
        reasonDetail: String? = null,
    ): SettlementReviewQueue {
        val existing = reviewQueueRepository.findByQuestionId(questionId)
        if (existing != null) {
            if (existing.status == SettlementReviewStatus.RESOLVED) {
                logger.warn("[ReviewQueue] 이미 RESOLVED된 항목 재등록 시도 questionId={}", questionId)
                return existing
            }
            existing.reasonCode = reasonCode
            existing.reasonDetail = reasonDetail
            existing.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
            logger.info("[ReviewQueue] questionId={} 사유 갱신: {}", questionId, reasonCode)
            return reviewQueueRepository.save(existing)
        }

        val entry = SettlementReviewQueue(
            questionId = questionId,
            reasonCode = reasonCode,
            reasonDetail = reasonDetail,
            maxRetry = DEFAULT_MAX_RETRY,
            nextRetryAt = nextRetryAt(0),
        )
        logger.info("[ReviewQueue] 신규 등록 questionId={} reason={}", questionId, reasonCode)
        return reviewQueueRepository.save(entry)
    }

    /**
     * 재시도 실패를 기록한다.
     * retryCount가 maxRetry에 도달하면 NEEDS_MANUAL로 자동 전환한다.
     */
    @Transactional
    fun markRetryFailed(entry: SettlementReviewQueue, error: String) {
        entry.retryCount += 1
        entry.lastTriedAt = LocalDateTime.now(ZoneOffset.UTC)
        entry.lastError = error.take(2000)  // TEXT 컬럼 과도한 저장 방지
        entry.updatedAt = LocalDateTime.now(ZoneOffset.UTC)

        if (entry.retryCount >= entry.maxRetry) {
            entry.status = SettlementReviewStatus.NEEDS_MANUAL
            logger.warn(
                "[ReviewQueue] questionId={} 재시도 한도({}) 초과 → NEEDS_MANUAL",
                entry.questionId, entry.maxRetry
            )
        } else {
            entry.nextRetryAt = nextRetryAt(entry.retryCount)
            logger.info(
                "[ReviewQueue] questionId={} 재시도 실패 ({}회/{}회), 다음 시도: {}",
                entry.questionId, entry.retryCount, entry.maxRetry, entry.nextRetryAt
            )
        }

        reviewQueueRepository.save(entry)
    }

    /**
     * 재시도 없이 즉시 수동처리 대기로 전환한다.
     */
    @Transactional
    fun markNeedsManual(entry: SettlementReviewQueue) {
        entry.status = SettlementReviewStatus.NEEDS_MANUAL
        entry.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        logger.warn("[ReviewQueue] questionId={} → NEEDS_MANUAL (즉시 전환)", entry.questionId)
        reviewQueueRepository.save(entry)
    }

    /**
     * 정산 완료(자동 또는 수동) 후 RESOLVED로 마킹한다.
     */
    @Transactional
    fun markResolved(questionId: Long) {
        val entry = reviewQueueRepository.findByQuestionId(questionId) ?: return
        entry.status = SettlementReviewStatus.RESOLVED
        entry.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        logger.info("[ReviewQueue] questionId={} → RESOLVED", questionId)
        reviewQueueRepository.save(entry)
    }

    fun findById(id: Long): SettlementReviewQueue =
        reviewQueueRepository.findById(id).orElseThrow {
            NotFoundException(
                message = ErrorCode.REVIEW_QUEUE_NOT_FOUND.message,
                code = ErrorCode.REVIEW_QUEUE_NOT_FOUND.name
            )
        }

    fun findAllByStatus(status: SettlementReviewStatus?): List<SettlementReviewQueue> =
        if (status != null) reviewQueueRepository.findAllByStatus(status)
        else reviewQueueRepository.findAll()

    fun findDueForRetry(now: LocalDateTime): List<SettlementReviewQueue> =
        reviewQueueRepository.findDueForRetry(now)

    // ─── private helpers ─────────────────────────────────────────────────────

    private fun nextRetryAt(currentRetryCount: Int): LocalDateTime {
        val delayMinutes = RETRY_DELAY_MINUTES.getOrElse(currentRetryCount) { RETRY_DELAY_MINUTES.last() }
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(delayMinutes)
    }
}
