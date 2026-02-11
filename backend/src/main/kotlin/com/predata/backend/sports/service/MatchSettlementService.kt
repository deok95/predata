package com.predata.backend.sports.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.SettlementService
import com.predata.backend.sports.domain.QuestionPhase
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class MatchSettlementService(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val settlementService: SettlementService
) {

    private val logger = LoggerFactory.getLogger(MatchSettlementService::class.java)

    /**
     * phase → FINISHED 전환 (별도 트랜잭션으로 즉시 커밋)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markPhaseFinished(questionId: Long) {
        val question = questionRepository.findById(questionId).orElse(null) ?: return
        question.phase = QuestionPhase.FINISHED
        questionRepository.save(question)
        logger.info("[MatchSettlement] phase=FINISHED 설정 - questionId={}", questionId)
    }

    /**
     * 경기 결과 기반 정산 (3회 재시도, 5초 간격)
     * initiateSettlement → finalizeSettlement → phase=SETTLED
     */
    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 5000))
    @Transactional
    fun settleQuestion(questionId: Long, finalResult: FinalResult) {
        logger.info("[MatchSettlement] 정산 시작 - questionId={}, result={}", questionId, finalResult)

        settlementService.initiateSettlement(questionId, finalResult, "MATCH_RESULT")
        settlementService.finalizeSettlement(questionId, skipDeadlineCheck = true)

        val question = questionRepository.findById(questionId).orElse(null)
        if (question != null) {
            question.phase = QuestionPhase.SETTLED
            questionRepository.save(question)
        }

        logger.info("[MatchSettlement] 정산 완료 - questionId={}, result={}", questionId, finalResult)
    }

    @Recover
    fun recoverSettleQuestion(e: Exception, questionId: Long, finalResult: FinalResult) {
        logger.error(
            "[MatchSettlement] 정산 최종 실패 (3회 재시도 소진) - questionId={}, result={}, error={}",
            questionId, finalResult, e.message, e
        )
        // phase = FINISHED 유지 → 어드민 수동 정산 필요
    }

    /**
     * 무승부 / POSTPONED / CANCELLED → 전액 환불
     * 모든 BET 활동에 대해 원금 반환
     */
    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 5000))
    @Transactional
    fun refundAllBets(questionId: Long, reason: String) {
        logger.info("[MatchSettlement] 전액 환불 시작 - questionId={}, reason={}", questionId, reason)

        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다: $questionId")

        if (question.status == QuestionStatus.SETTLED && question.phase == QuestionPhase.SETTLED) {
            logger.warn("[MatchSettlement] 이미 정산 완료된 질문 - questionId={}", questionId)
            return
        }

        val bets = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.BET)

        val memberIds = bets.map { it.memberId }.distinct()
        val membersMap = if (memberIds.isNotEmpty()) {
            memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        var refundCount = 0
        var totalRefunded = 0L

        bets.forEach { bet ->
            // 이미 판매된 베팅은 환불 제외
            val sold = activityRepository.findByParentBetIdAndActivityType(bet.id!!, ActivityType.BET_SELL)
            if (sold == null) {
                val member = membersMap[bet.memberId]
                if (member != null) {
                    member.pointBalance += bet.amount
                    refundCount++
                    totalRefunded += bet.amount
                }
            }
        }

        question.status = QuestionStatus.SETTLED
        question.phase = QuestionPhase.SETTLED
        questionRepository.save(question)

        logger.info(
            "[MatchSettlement] 전액 환불 완료 - questionId={}, reason={}, refundCount={}, totalRefunded={}",
            questionId, reason, refundCount, totalRefunded
        )
    }

    @Recover
    fun recoverRefundAllBets(e: Exception, questionId: Long, reason: String) {
        logger.error(
            "[MatchSettlement] 환불 최종 실패 (3회 재시도 소진) - questionId={}, reason={}, error={}",
            questionId, reason, e.message, e
        )
        // phase = FINISHED 유지 → 어드민 수동 정산 필요
    }

    /**
     * 어드민 수동 정산 (자동 정산 실패 시 사용)
     */
    @Transactional
    fun manualSettle(questionId: Long, result: String): String {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다: $questionId")

        if (question.phase == QuestionPhase.SETTLED) {
            throw IllegalStateException("이미 정산 완료된 질문입니다.")
        }

        return when (result.uppercase()) {
            "YES" -> {
                settlementService.initiateSettlement(questionId, FinalResult.YES, "ADMIN_MANUAL")
                settlementService.finalizeSettlement(questionId, skipDeadlineCheck = true)
                question.phase = QuestionPhase.SETTLED
                questionRepository.save(question)
                "수동 정산 완료 (YES 승)"
            }
            "NO" -> {
                settlementService.initiateSettlement(questionId, FinalResult.NO, "ADMIN_MANUAL")
                settlementService.finalizeSettlement(questionId, skipDeadlineCheck = true)
                question.phase = QuestionPhase.SETTLED
                questionRepository.save(question)
                "수동 정산 완료 (NO 승)"
            }
            "DRAW" -> {
                refundAllBets(questionId, "ADMIN_MANUAL_DRAW")
                "수동 정산 완료 (무승부 - 전액 환불)"
            }
            else -> throw IllegalArgumentException("결과는 YES, NO, DRAW만 가능합니다.")
        }
    }
}
