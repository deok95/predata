package com.predata.backend.service

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteCommitStatus
import com.predata.backend.domain.VotingPhase
import com.predata.backend.repository.AuditLogRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteCommitRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 알림 서비스
 * - 분배 실패 3회 이상 시 ALERT 레벨 기록
 * - 미리빌 투표가 reveal 마감 1시간 전까지 50% 이상이면 경고 로그
 * - 스케줄러에서 5분마다 실행
 */
@Service
class AlertService(
    private val questionRepository: QuestionRepository,
    private val voteCommitRepository: VoteCommitRepository,
    private val auditLogRepository: AuditLogRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(AlertService::class.java)

    companion object {
        const val DISTRIBUTION_FAILURE_THRESHOLD = 3  // 분배 실패 3회 이상
        const val REVEAL_RATE_THRESHOLD = 0.5  // 미리빌율 50% 이상
        const val REVEAL_DEADLINE_HOURS = 1L  // reveal 마감 1시간 전
    }

    // 이미 알림을 보낸 질문 ID 추적 (중복 알림 방지)
    private val alertedQuestions = mutableSetOf<Long>()

    /**
     * 알림 체크 및 기록
     * - 5분마다 자동 실행
     */
    @Scheduled(fixedRate = 300000)  // 5분 = 300,000ms
    fun checkAndAlert() {
        logger.debug("AlertService: Starting alert check...")

        try {
            // 1. 분배 실패 3회 이상 체크
            checkDistributionFailures()

            // 2. 미리빌 투표 50% 이상 체크 (reveal 마감 1시간 전)
            checkUnrevealedVotes()

        } catch (e: Exception) {
            logger.error("AlertService: Error during alert check", e)
        }
    }

    /**
     * 분배 실패 3회 이상 체크
     */
    private fun checkDistributionFailures() {
        try {
            // 최근 분배 실패 건수 조회
            val failureCount = auditLogRepository.countByActionAndDetailContaining(
                AuditAction.REWARD_FAILED,
                "실패"
            )

            if (failureCount >= DISTRIBUTION_FAILURE_THRESHOLD) {
                logger.error("ALERT: 보상 분배 실패가 ${failureCount}건 발생했습니다.")

                // AuditLog에 ALERT 기록 (SYSTEM_ALERT 액션이 없으면 일반 로그만)
                try {
                    auditService.log(
                        memberId = null,  // 시스템 알림
                        action = AuditAction.REWARD_FAILED,  // 관련 액션
                        entityType = "ALERT",
                        entityId = null,
                        detail = "ALERT: 보상 분배 실패 ${failureCount}건 발생"
                    )
                } catch (e: Exception) {
                    logger.error("Failed to log distribution failure alert", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error checking distribution failures", e)
        }
    }

    /**
     * 미리빌 투표 50% 이상 체크
     */
    private fun checkUnrevealedVotes() {
        val now = LocalDateTime.now(ZoneOffset.UTC)

        // VOTING_REVEAL_OPEN 상태의 질문들 조회
        val revealPhaseQuestions = questionRepository.findByStatus(QuestionStatus.VOTING)
            .filter { it.votingPhase == VotingPhase.VOTING_REVEAL_OPEN }

        for (question in revealPhaseQuestions) {
            try {
                // reveal 마감 시간 확인 (votingEndAt이 reveal 마감 시간이라고 가정)
                val revealDeadline = question.votingEndAt
                val oneHourBeforeDeadline = revealDeadline.minusHours(REVEAL_DEADLINE_HOURS)

                // 아직 마감 1시간 전이 아니면 스킵
                if (now.isBefore(oneHourBeforeDeadline)) {
                    continue
                }

                // 이미 알림을 보낸 질문이면 스킵
                if (alertedQuestions.contains(question.id)) {
                    continue
                }

                // 투표 통계 조회
                val committedCount = voteCommitRepository.countByQuestionIdAndStatus(
                    question.id!!,
                    VoteCommitStatus.COMMITTED
                )
                val revealedCount = voteCommitRepository.countByQuestionIdAndStatus(
                    question.id!!,
                    VoteCommitStatus.REVEALED
                )
                val totalVotes = committedCount + revealedCount

                if (totalVotes == 0L) continue

                // 미리빌율 계산
                val unrevealedRate = committedCount.toDouble() / totalVotes.toDouble()

                // 미리빌율이 50% 이상이면 경고
                if (unrevealedRate >= REVEAL_RATE_THRESHOLD) {
                    logger.warn(
                        "ALERT: 질문 ${question.id}의 미리빌 투표가 ${(unrevealedRate * 100).toInt()}%입니다. " +
                                "(미리빌: $committedCount, 공개: $revealedCount, 총: $totalVotes)"
                    )

                    // 알림 기록
                    try {
                        auditService.log(
                            memberId = null,
                            action = AuditAction.VOTE_REVEAL,
                            entityType = "ALERT",
                            entityId = question.id,
                            detail = "ALERT: 미리빌율 ${(unrevealedRate * 100).toInt()}% (미리빌: $committedCount/${totalVotes})"
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to log unrevealed votes alert", e)
                    }

                    // 중복 알림 방지
                    alertedQuestions.add(question.id!!)
                }
            } catch (e: Exception) {
                logger.error("Error checking unrevealed votes for question ${question.id}", e)
            }
        }

        // 오래된 알림 기록 정리 (메모리 누수 방지)
        if (alertedQuestions.size > 1000) {
            alertedQuestions.clear()
            logger.info("AlertService: Cleared alerted questions set")
        }
    }
}
