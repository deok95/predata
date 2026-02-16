package com.predata.backend.service

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteCommitStatus
import com.predata.backend.repository.AuditLogRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteCommitRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 투표 대시보드 서비스
 * - 전체 시스템 현황 조회
 * - 질문별 상태 조회
 */
@Service
class VotingDashboardService(
    private val voteCommitRepository: VoteCommitRepository,
    private val questionRepository: QuestionRepository,
    private val auditLogRepository: AuditLogRepository,
    private val circuitBreaker: VotingCircuitBreaker,
    private val pauseService: PauseService
) {

    /**
     * 전체 대시보드 현황
     */
    fun getDashboard(): Map<String, Any> {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val oneDayAgo = now.minusHours(24)

        // 1. 미리빌(reveal 안 한) 투표 수
        val unrevealedVotes = voteCommitRepository.countByStatus(VoteCommitStatus.COMMITTED)

        // 2. 미정산 질문 수 (SETTLED가 아닌 질문들)
        val unsettledQuestions = questionRepository.countByStatusNot(QuestionStatus.SETTLED)

        // 3. 분배 실패 건수 (AuditLog에서 실패 관련 조회)
        val distributionFailures = try {
            auditLogRepository.countByActionAndDetailContaining(
                AuditAction.REWARD_FAILED,
                "실패"
            )
        } catch (e: Exception) {
            0L  // 해당 액션이 없으면 0
        }

        // 4. 재시도 큐 대기 건수 (현재는 큐가 없으므로 0)
        val retryQueueSize = 0L

        // 5. 서킷브레이커 상태
        val circuitBreakerStatus = circuitBreaker.getStats()

        // 6. 일시 중지 상태
        val pauseStatus = pauseService.getAllPauseStatus()

        // 7. 최근 24시간 통계
        val last24hVoteCommits = auditLogRepository.countByActionAndCreatedAtBetween(
            AuditAction.VOTE_COMMIT,
            oneDayAgo,
            now
        )

        val last24hVoteReveals = auditLogRepository.countByActionAndCreatedAtBetween(
            AuditAction.VOTE_REVEAL,
            oneDayAgo,
            now
        )

        val last24hSettlements = try {
            auditLogRepository.countByActionAndCreatedAtBetween(
                AuditAction.SETTLE,
                oneDayAgo,
                now
            )
        } catch (e: Exception) {
            0L
        }

        val last24hDistributions = try {
            auditLogRepository.countByActionAndCreatedAtBetween(
                AuditAction.REWARD_DISTRIBUTED,
                oneDayAgo,
                now
            )
        } catch (e: Exception) {
            0L
        }

        return mapOf(
            "overview" to mapOf(
                "unrevealedVotes" to unrevealedVotes,
                "unsettledQuestions" to unsettledQuestions,
                "distributionFailures" to distributionFailures,
                "retryQueueSize" to retryQueueSize
            ),
            "circuitBreaker" to circuitBreakerStatus,
            "pauseStatus" to pauseStatus,
            "last24Hours" to mapOf(
                "voteCommits" to last24hVoteCommits,
                "voteReveals" to last24hVoteReveals,
                "settlements" to last24hSettlements,
                "distributions" to last24hDistributions
            ),
            "timestamp" to now.toString()
        )
    }

    /**
     * 특정 질문의 상태 조회
     */
    fun getQuestionHealth(questionId: Long): Map<String, Any> {
        val question = questionRepository.findById(questionId).orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "질문을 찾을 수 없습니다."
            )

        // 투표 통계
        val committedCount = voteCommitRepository.countByQuestionIdAndStatus(questionId, VoteCommitStatus.COMMITTED)
        val revealedCount = voteCommitRepository.countByQuestionIdAndStatus(questionId, VoteCommitStatus.REVEALED)
        val totalVotes = committedCount + revealedCount

        // 미리빌율 계산
        val revealRate = if (totalVotes > 0) {
            (revealedCount.toDouble() / totalVotes.toDouble() * 100).toInt()
        } else {
            0
        }

        // 중지 상태
        val isPaused = pauseService.isPaused(questionId)

        return mapOf(
            "success" to true,
            "questionId" to questionId,
            "status" to question.status.name,
            "votingPhase" to question.votingPhase.name,
            "votes" to mapOf(
                "committed" to committedCount,
                "revealed" to revealedCount,
                "total" to totalVotes,
                "revealRate" to "$revealRate%"
            ),
            "isPaused" to isPaused,
            "finalResult" to question.finalResult.name
        )
    }
}
