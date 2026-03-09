package com.predata.backend.service.market

import com.predata.backend.config.properties.MarketBatchProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * 마켓 배치 스케줄러
 *
 * - 단일 스케줄러 → 순서 보장, 경합 제거
 * - 5분 주기 (app.market-batch.cron-expression 로 재정의 가능)
 * - cutoff_slot_utc UNIQUE 제약으로 중복 실행 방지
 */
@Component
class MarketBatchScheduler(
    private val marketBatchService: MarketBatchService,
    private val properties: MarketBatchProperties,
) {
    private val logger = LoggerFactory.getLogger(MarketBatchScheduler::class.java)

    @Scheduled(cron = "\${app.market-batch.cron-expression:0 */5 * * * *}")
    fun runScheduled() {
        if (!properties.enabled) {
            logger.debug("[MarketBatchScheduler] 비활성화 상태 (app.market-batch.enabled=false). 스킵.")
            return
        }

        val cutoffSlot = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES)
        logger.info("[MarketBatchScheduler] 배치 실행 시작. cutoffSlot=$cutoffSlot")

        try {
            marketBatchService.runBatch(cutoffSlot)
        } catch (e: Exception) {
            logger.error("[MarketBatchScheduler] 배치 실행 중 예외 발생: ${e.message}", e)
        }
    }
}
