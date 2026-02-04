package com.predata.backend.service

import com.predata.backend.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QuestionLifecycleScheduler(
    private val questionRepository: QuestionRepository,
    private val settlementService: SettlementService
) {
    private val logger = LoggerFactory.getLogger(QuestionLifecycleScheduler::class.java)

    /**
     * 1분마다 실행:
     * - 만료된 OPEN 질문 → CLOSED
     * - disputeDeadline 지난 PENDING_SETTLEMENT → 자동 정산 확정
     */
    @Scheduled(cron = "0 * * * * *")
    fun processQuestionLifecycle() {
        closeExpiredQuestions()
        finalizePastDueSettlements()
    }

    @Transactional
    fun closeExpiredQuestions() {
        val now = LocalDateTime.now()
        val expiredQuestions = questionRepository.findOpenExpiredBefore(now)
        if (expiredQuestions.isEmpty()) return

        logger.info("[Lifecycle] 만료된 OPEN 질문 {}건 발견", expiredQuestions.size)
        expiredQuestions.forEach { question ->
            try {
                val locked = questionRepository.findByIdWithLock(question.id!!)
                if (locked != null && locked.status == "OPEN" && locked.expiredAt.isBefore(now)) {
                    locked.status = "CLOSED"
                    questionRepository.save(locked)
                    logger.info("[Lifecycle] 질문 #{} '{}' → CLOSED", locked.id, locked.title)
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] 질문 #{} CLOSED 전환 실패: {}", question.id, e.message)
            }
        }
    }

    @Transactional
    fun finalizePastDueSettlements() {
        val now = LocalDateTime.now()
        val pendingQuestions = questionRepository.findPendingSettlementPastDeadline(now)
        if (pendingQuestions.isEmpty()) return

        logger.info("[Lifecycle] 이의제기 만료 PENDING_SETTLEMENT 질문 {}건 발견", pendingQuestions.size)
        pendingQuestions.forEach { question ->
            try {
                val result = settlementService.finalizeSettlement(question.id!!)
                logger.info("[Lifecycle] 질문 #{} 자동 정산 확정 (배당: {}P)", question.id, result.totalPayout)
            } catch (e: Exception) {
                logger.error("[Lifecycle] 질문 #{} 자동 정산 실패: {}", question.id, e.message)
            }
        }
    }
}
