package com.predata.backend.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class QuestionGeneratorScheduler(
    private val questionGeneratorService: QuestionGeneratorService
) {
    private val logger = LoggerFactory.getLogger(QuestionGeneratorScheduler::class.java)

    private val lastExecutionTime = AtomicLong(0)

    /**
     * 매분 실행하여 질문 생성 여부 확인
     * - enabled = true일 때만 동작
     * - interval 간격 준수
     * - 설정된 카테고리 중 랜덤 선택
     */
    @Scheduled(fixedDelay = 60000)
    fun checkAndGenerateQuestion() {
        try {
            if (!questionGeneratorService.isEnabled()) {
                return
            }

            val intervalSeconds = questionGeneratorService.getIntervalSeconds()
            val now = System.currentTimeMillis()
            val lastExec = lastExecutionTime.get()

            if (lastExec > 0 && (now - lastExec) < (intervalSeconds * 1000)) {
                return
            }

            logger.info("[QuestionGeneratorScheduler] ===== 자동 질문 생성 시작 =====")

            val categories = questionGeneratorService.getCategories()
            val randomCategory = categories.randomOrNull()

            logger.info("[QuestionGeneratorScheduler] 선택된 카테고리: $randomCategory")

            val result = questionGeneratorService.generateQuestion(randomCategory)

            if (result.success) {
                lastExecutionTime.set(now)
                logger.info("[QuestionGeneratorScheduler] 생성 완료: ${result.title}")
            } else {
                logger.warn("[QuestionGeneratorScheduler] 생성 실패: ${result.message}")
            }

        } catch (e: Exception) {
            logger.error("[QuestionGeneratorScheduler] 스케줄러 오류: ${e.message}", e)
        }
    }
}
