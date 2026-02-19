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
    @Value("\${app.questiongen.enabled:false}") private val appEnabled: Boolean,
    @Value("\${app.questiongen.daily-batch-enabled:false}") private val dailyBatchEnabled: Boolean,
    @Value("\${app.questiongen.daily-trend-enabled:true}") private val dailyTrendEnabled: Boolean
) {

    private val logger = LoggerFactory.getLogger(AutoQuestionGenerationScheduler::class.java)

    @Scheduled(cron = "\${app.questiongen.daily-trend-cron:0 0 9 * * *}")
    fun runDailyTrendQuestion() {
        if (!appEnabled || !dailyTrendEnabled) {
            logger.debug("[AutoQuestionScheduler] daily trend generation disabled - skip")
            return
        }

        try {
            val result = autoQuestionGenerationService.generateDailyTrendQuestion()
            if (result.success) {
                logger.info("[AutoQuestionScheduler] daily trend question created: id={}, title={}", result.questionId, result.title)
            } else {
                logger.warn("[AutoQuestionScheduler] daily trend generation skipped: {}", result.message)
            }
        } catch (e: Exception) {
            logger.error("[AutoQuestionScheduler] daily trend generation failed: {}", e.message, e)
        }
    }

    @Scheduled(cron = "\${app.questiongen.daily-cron:0 5 0 * * *}")
    fun runDailyBatch() {
        if (!dailyBatchEnabled) {
            logger.debug("[AutoQuestionScheduler] daily batch generation disabled - skip")
            return
        }

        val settings = settingsService.get()
        if (!appEnabled || !settings.autoEnabled) {
            logger.debug("[AutoQuestionScheduler] auto-generation disabled in settings - skip")
            return
        }

        logger.info("[AutoQuestionScheduler] starting legacy daily batch generation - subcategories={}", settings.subcategories)

        settings.subcategories.forEach { subcategory ->
            try {
                val response = autoQuestionGenerationService.generateBatch(
                    BatchGenerateQuestionsRequest(
                        subcategory = subcategory,
                        dryRun = false
                    )
                )
                logger.info(
                    "[AutoQuestionScheduler] legacy batch done: subcategory={}, accepted={}, rejected={}",
                    subcategory,
                    response.acceptedCount,
                    response.rejectedCount
                )
            } catch (e: Exception) {
                logger.error("[AutoQuestionScheduler] Failed: subcategory={}, error={}", subcategory, e.message, e)
            }
        }
    }
}
