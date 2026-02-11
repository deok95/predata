package com.predata.backend.sports.event

import com.predata.backend.domain.FinalResult
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.service.MatchSettlementService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MatchSettlementListener(
    private val questionRepository: QuestionRepository,
    private val matchSettlementService: MatchSettlementService
) {

    private val logger = LoggerFactory.getLogger(MatchSettlementListener::class.java)

    /**
     * 경기 종료 → 자동 정산
     * homeScore > awayScore → YES 승
     * homeScore < awayScore → NO 승
     * 무승부 → 전액 환불
     */
    @EventListener
    fun onMatchFinished(event: MatchFinishedEvent) {
        logger.info(
            "[MatchSettlement] 경기 종료 이벤트 수신 - matchId={}, score={}-{}",
            event.matchId, event.homeScore, event.awayScore
        )

        val questions = questionRepository.findByMatchId(event.matchId)
        if (questions.isEmpty()) {
            logger.info("[MatchSettlement] 연결된 질문 없음 - matchId={}", event.matchId)
            return
        }

        for (question in questions) {
            val questionId = question.id ?: continue

            try {
                // phase → FINISHED (별도 트랜잭션)
                matchSettlementService.markPhaseFinished(questionId)

                when {
                    event.homeScore > event.awayScore -> {
                        logger.info("[MatchSettlement] 홈팀 승리 → YES 정산 - questionId={}", questionId)
                        matchSettlementService.settleQuestion(questionId, FinalResult.YES)
                    }
                    event.homeScore < event.awayScore -> {
                        logger.info("[MatchSettlement] 원정팀 승리 → NO 정산 - questionId={}", questionId)
                        matchSettlementService.settleQuestion(questionId, FinalResult.NO)
                    }
                    else -> {
                        logger.info("[MatchSettlement] 무승부 → 전액 환불 - questionId={}", questionId)
                        matchSettlementService.refundAllBets(questionId, "DRAW")
                    }
                }
            } catch (e: Exception) {
                // @Recover가 처리하지 못한 예외 (이미 정산된 경우 등)
                logger.error(
                    "[MatchSettlement] 정산 처리 실패 - questionId={}, error={}",
                    questionId, e.message, e
                )
            }
        }
    }

    /**
     * 경기 연기/취소 → 전액 환불
     */
    @EventListener
    fun onMatchCancelled(event: MatchCancelledEvent) {
        logger.info(
            "[MatchSettlement] 경기 취소/연기 이벤트 수신 - matchId={}, status={}",
            event.matchId, event.matchStatus
        )

        val questions = questionRepository.findByMatchId(event.matchId)
        if (questions.isEmpty()) {
            logger.info("[MatchSettlement] 연결된 질문 없음 - matchId={}", event.matchId)
            return
        }

        for (question in questions) {
            val questionId = question.id ?: continue

            try {
                matchSettlementService.markPhaseFinished(questionId)
                matchSettlementService.refundAllBets(questionId, event.matchStatus.name)
            } catch (e: Exception) {
                logger.error(
                    "[MatchSettlement] 환불 처리 실패 - questionId={}, error={}",
                    questionId, e.message, e
                )
            }
        }
    }
}
