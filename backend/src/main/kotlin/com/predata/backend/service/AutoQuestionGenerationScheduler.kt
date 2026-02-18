package com.predata.backend.service

import com.predata.backend.dto.BatchGenerateQuestionsRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["app.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class AutoQuestionGenerationScheduler(
    private val autoQuestionGenerationService: AutoQuestionGenerationService,
    private val settingsService: QuestionGenerationSettingsService,
    @Value("\${app.questiongen.enabled:false}") private val appEnabled: Boolean
) {

    private val logger = LoggerFactory.getLogger(AutoQuestionGenerationScheduler::class.java)

    /**
     * 하루 1회 subcategory별 자동 생성
     * 기본: UTC 00:05
     */
    @Scheduled(cron = "\${app.questiongen.daily-cron:0 5 0 * * *}")
    fun runDailyBatch() {
        val settings = settingsService.get()
        if (!appEnabled || !settings.autoEnabled) {
            logger.debug("[AutoQuestionScheduler] 비활성 상태 - skip")
            return
        }

        logger.info("[AutoQuestionScheduler] 일일 자동생성 시작 - subcategories={}", settings.subcategories)

        settings.subcategories.forEach { subcategory ->
            try {
                val response = autoQuestionGenerationService.generateBatch(
                    BatchGenerateQuestionsRequest(
                        subcategory = subcategory,
                        dryRun = false
                    )
                )
                logger.info(
                    "[AutoQuestionScheduler] 완료: subcategory={}, accepted={}, rejected={}",
                    subcategory,
                    response.acceptedCount,
                    response.rejectedCount
                )
            } catch (e: Exception) {
                logger.error("[AutoQuestionScheduler] 실패: subcategory={}, error={}", subcategory, e.message, e)
            }
        }
    }
}
