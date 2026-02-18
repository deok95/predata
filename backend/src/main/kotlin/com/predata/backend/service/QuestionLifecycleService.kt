package com.predata.backend.service

import com.predata.backend.domain.VotingPhase
import com.predata.backend.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 질문 생명주기 관리 서비스
 * - 투표 단계 전환 (FSM)
 */
@Service
class QuestionLifecycleService(
    private val questionRepository: QuestionRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(QuestionLifecycleService::class.java)

    /**
     * 투표 단계 전환 (FSM 검증)
     * - 허용된 전환만 수행
     * - 잘못된 전환 시도는 AuditLog 기록 후 false 반환
     */
    @Transactional
    fun transitionTo(questionId: Long, newPhase: VotingPhase): Boolean {
        val question = questionRepository.findById(questionId).orElse(null)
            ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        val currentPhase = question.votingPhase

        // FSM 전환 검증
        val allowed = when (currentPhase) {
            VotingPhase.VOTING_COMMIT_OPEN -> newPhase == VotingPhase.VOTING_REVEAL_OPEN
            VotingPhase.VOTING_REVEAL_OPEN -> newPhase == VotingPhase.VOTING_REVEAL_CLOSED || newPhase == VotingPhase.BETTING_OPEN
            VotingPhase.VOTING_REVEAL_CLOSED -> newPhase == VotingPhase.BETTING_OPEN
            VotingPhase.BETTING_OPEN -> newPhase == VotingPhase.SETTLEMENT_PENDING
            VotingPhase.SETTLEMENT_PENDING -> newPhase == VotingPhase.SETTLED
            VotingPhase.SETTLED -> newPhase == VotingPhase.REWARD_DISTRIBUTED
            VotingPhase.REWARD_DISTRIBUTED -> false  // 종료 상태
        }

        if (!allowed) {
            logger.warn("Invalid phase transition: questionId=$questionId, $currentPhase -> $newPhase")
            auditService.log(
                memberId = null,
                action = com.predata.backend.domain.AuditAction.RISK_LIMIT_EXCEEDED,
                entityType = "QUESTION",
                entityId = questionId,
                detail = "Invalid phase transition: $currentPhase -> $newPhase"
            )
            return false
        }

        question.votingPhase = newPhase
        questionRepository.save(question)
        logger.info("Phase transition successful: questionId=$questionId, $currentPhase -> $newPhase")
        return true
    }
}
