package com.predata.backend.sports.scheduler

import com.predata.backend.domain.QuestionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.domain.QuestionPhase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

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
     *   pre-match      → BETTING
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

        val now = LocalDateTime.now(ZoneOffset.UTC)
        var transitioned = 0

        for (question in questions) {
            val match = question.match ?: continue
            val matchTime = match.matchTime
            val currentPhase = question.phase

            val newPhase = when (match.matchStatus) {
                MatchStatus.LIVE, MatchStatus.HALFTIME -> QuestionPhase.LIVE
                MatchStatus.FINISHED, MatchStatus.CANCELLED, MatchStatus.POSTPONED -> QuestionPhase.FINISHED
                else -> if (!now.isBefore(matchTime)) QuestionPhase.LIVE else QuestionPhase.BETTING
            }

            if (newPhase != currentPhase) {
                val oldPhase = currentPhase?.name ?: "NULL"
                question.phase = newPhase

                // phase에 맞춰 status도 동기화
                when (newPhase) {
                    QuestionPhase.BETTING -> {
                        question.status = QuestionStatus.BETTING
                        question.category = "SPORTS"
                    }
                    QuestionPhase.LIVE -> {
                        // LIVE 탭 노출용 카테고리 전환 (match 연결 질문만)
                        question.status = QuestionStatus.BETTING
                        question.category = "LIVE"
                    }
                    QuestionPhase.FINISHED -> {
                        // 종료된 경기는 LIVE에서 즉시 비노출 (정산 완료 전/후 공통)
                        question.category = "SPORTS"
                    }
                    else -> {}
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
