package com.predata.backend.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    prefix = "sports.legacy-scheduler",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class SportsSchedulerService(
    private val questionAutoGenerationService: QuestionAutoGenerationService
) {

    /**
     * 매일 자정에 다가오는 경기 가져와서 질문 생성
     * Cron: 0 0 0 * * * (초 분 시 일 월 요일)
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduledQuestionGeneration() {
        println("[Scheduler] ===== 자동 질문 생성 시작 =====")
        try {
            val result = questionAutoGenerationService.generateSportsQuestions()
            println("[Scheduler] 자동 질문 생성 완료: 생성 ${result.created}건, 스킵 ${result.skipped}건")
        } catch (e: Exception) {
            println("[Scheduler] Auto question generation failed: ${e.message}")
        }
    }

    /**
     * 초단위 실시간 스코어 업데이트 (30초마다)
     * Cron: 0,30 * * * * * (매 분 0초, 30초)
     */
    @Scheduled(cron = "0,30 * * * * *")
    fun scheduledMatchResultUpdate() {
        println("[Scheduler] ===== 🔴 실시간 스코어 업데이트 시작 =====")
        try {
            val result = questionAutoGenerationService.updateFinishedMatches()
            if (result.updated > 0) {
                println("[Scheduler] ✅ 실시간 스코어 업데이트 완료: ${result.updated}건")
            }
        } catch (e: Exception) {
            println("[Scheduler] Real-time score update failed: ${e.message}")
        }
    }

    /**
     * 즉시 자동 정산 (1분마다) - 경기 종료 후 즉시 정산
     * Cron: 0 * * * * * (1분마다)
     */
    @Scheduled(cron = "0 * * * * *")
    fun scheduledAutoSettlement() {
        println("[Scheduler] ===== ⚡ 즉시 자동 정산 체크 =====")
        try {
            val result = questionAutoGenerationService.autoSettleSportsQuestions()
            if (result.settled > 0) {
                println("[Scheduler] ✅ 자동 정산 완료: ${result.settled}건")
            }
        } catch (e: Exception) {
            println("[Scheduler] Auto-settlement failed: ${e.message}")
        }
    }

    /**
     * 테스트용: 애플리케이션 시작 5분 후 한 번 실행
     */
    @Scheduled(initialDelay = 300000, fixedDelay = Long.MAX_VALUE)
    fun initialTestRun() {
        println("[Scheduler] ===== 초기 테스트 실행 =====")
        try {
            // 1. 질문 생성
            val genResult = questionAutoGenerationService.generateSportsQuestions()
            println("[Scheduler] 테스트 질문 생성: ${genResult.created}건")
            
            // 2. 결과 업데이트
            val updateResult = questionAutoGenerationService.updateFinishedMatches()
            println("[Scheduler] 테스트 결과 업데이트: ${updateResult.updated}건")
            
            // 3. 자동 정산
            val settleResult = questionAutoGenerationService.autoSettleSportsQuestions()
            println("[Scheduler] 테스트 자동 정산: ${settleResult.settled}건")
        } catch (e: Exception) {
            println("[Scheduler] Test execution failed: ${e.message}")
        }
    }
}
