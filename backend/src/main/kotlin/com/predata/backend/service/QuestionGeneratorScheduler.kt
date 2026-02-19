package com.predata.backend.service

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    prefix = "app.questiongen",
    name = ["legacy-interval-enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class QuestionGeneratorScheduler {
    private val logger = LoggerFactory.getLogger(QuestionGeneratorScheduler::class.java)

    /**
     * 매분 실행하여 질문 생성 여부 확인
     * - enabled = true일 때만 동작
     * - interval 간격 준수
     * - 설정된 카테고리 중 랜덤 선택
     */
    @Scheduled(fixedDelay = 60000)
    fun checkAndGenerateQuestion() {
        logger.warn("[QuestionGeneratorScheduler] Legacy interval scheduler is disabled. Daily trend scheduler must be used.")
    }
}
