package com.predata.backend.sports.scheduler

import com.predata.backend.domain.QuestionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.domain.QuestionPhase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(
    name = ["app.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class QuestionPhaseScheduler(
    private val questionRepository: QuestionRepository
) {

    private val logger = LoggerFactory.getLogger(QuestionPhaseScheduler::class.java)

    /**
     * 5분 간격으로 Match 연결 Question의 phase 자동 전환
     *   matchTime - 48h → VOTING
     *   matchTime - 24h → BETTING
     *   matchTime      → LIVE
     */
    @Scheduled(fixedDelayString = "\${sports.scheduler.phase-check-interval-ms}")
    @Transactional
    fun checkPhaseTransitions() {
        val questions = questionRepository.findMatchQuestionsForPhaseCheck()
        if (questions.isEmpty()) {
            logger.debug("[PhaseScheduler] phase 전환 대상 없음, skip")
            return
        }

        val now = LocalDateTime.now()
        var transitioned = 0

        for (question in questions) {
            val match = question.match ?: continue
            val matchTime = match.matchTime
            val currentPhase = question.phase

            val newPhase = when {
                !now.isBefore(matchTime) -> QuestionPhase.LIVE
                !now.isBefore(matchTime.minusHours(24)) -> QuestionPhase.BETTING
                !now.isBefore(matchTime.minusHours(48)) -> QuestionPhase.VOTING
                else -> null
            }

            if (newPhase != null && newPhase != currentPhase) {
                val oldPhase = currentPhase?.name ?: "NULL"
                question.phase = newPhase

                // phase에 맞춰 status도 동기화
                when (newPhase) {
                    QuestionPhase.VOTING -> question.status = QuestionStatus.VOTING
                    QuestionPhase.BETTING -> question.status = QuestionStatus.BETTING
                    else -> {} // LIVE에서는 status 변경 안 함
                }

                questionRepository.save(question)
                transitioned++

                logger.info(
                    "[PhaseScheduler] 전환: {} | {} → {} | match={}",
                    question.title, oldPhase, newPhase, match.id
                )
            }
        }

        if (transitioned > 0) {
            logger.info("[PhaseScheduler] phase 전환 완료: ${transitioned}건")
        }
    }
}
