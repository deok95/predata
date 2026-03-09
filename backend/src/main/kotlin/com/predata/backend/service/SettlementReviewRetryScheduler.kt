package com.predata.backend.service

import com.predata.backend.domain.SettlementReviewReasonCode
import com.predata.backend.domain.SettlementReviewStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 정산 검토 큐 자동 재시도 스케줄러.
 *
 * 5분마다 PENDING_RETRY 항목을 꺼내 autoSettleWithVerification을 재시도한다.
 * - 성공: markResolved
 * - 실패: markRetryFailed (한도 초과 시 NEEDS_MANUAL 자동 전환)
 */
@Service
@ConditionalOnProperty(
    name = ["app.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class SettlementReviewRetryScheduler(
    private val reviewQueueService: SettlementReviewQueueService,
    private val settlementAutomationService: SettlementAutomationService,
) {
    private val logger = LoggerFactory.getLogger(SettlementReviewRetryScheduler::class.java)

    @Scheduled(fixedDelayString = "\${app.settlement.retry-interval-ms:300000}")
    fun retryPendingSettlements() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val dueEntries = reviewQueueService.findDueForRetry(now)

        if (dueEntries.isEmpty()) return

        logger.info("[RetryScheduler] 재시도 대상 {}건 처리 시작", dueEntries.size)

        dueEntries.forEach { entry ->
            try {
                val result = settlementAutomationService.autoSettleWithVerification(entry.questionId)
                if (result != null) {
                    logger.info(
                        "[RetryScheduler] questionId={} 자동정산 성공 (결과: {})",
                        entry.questionId, result.finalResult
                    )
                    reviewQueueService.markResolved(entry.questionId)
                } else {
                    logger.info("[RetryScheduler] questionId={} 자동정산 조건 미충족, 재시도 예정", entry.questionId)
                    reviewQueueService.markRetryFailed(entry, "autoSettle returned null")
                }
            } catch (e: Exception) {
                logger.error(
                    "[RetryScheduler] questionId={} 재시도 예외: {}",
                    entry.questionId, e.message
                )
                reviewQueueService.markRetryFailed(
                    entry,
                    "${SettlementReviewReasonCode.EXCEPTION}: ${e.message?.take(500)}"
                )
            }
        }

        val resolved = dueEntries.count {
            reviewQueueService.findAllByStatus(SettlementReviewStatus.RESOLVED)
                .any { r -> r.questionId == it.questionId }
        }
        logger.info("[RetryScheduler] 처리 완료 — 성공: {}건 / 전체: {}건", resolved, dueEntries.size)
    }
}
