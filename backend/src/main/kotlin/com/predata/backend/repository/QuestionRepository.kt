package com.predata.backend.repository

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType
import java.time.LocalDateTime

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {

    fun findByStatus(status: QuestionStatus): List<Question>

    /**
     * 특정 상태가 아닌 질문 개수 (대시보드용)
     */
    fun countByStatusNot(status: QuestionStatus): Long

    // Legacy queries (kept for compatibility)
    @Query("SELECT q FROM Question q WHERE q.status = 'OPEN' AND q.expiredAt < :time")
    fun findOpenExpiredBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'SETTLED' AND q.finalResult != 'PENDING' AND q.disputeDeadline IS NOT NULL AND q.disputeDeadline < :deadline")
    fun findPendingSettlementPastDeadline(deadline: LocalDateTime): List<Question>

    // New status transition queries
    @Query("SELECT q FROM Question q WHERE q.status = 'VOTING' AND q.votingEndAt <= :time")
    fun findVotingExpiredBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'BREAK' AND q.bettingStartAt < :time")
    fun findBreakExpiredBefore(time: LocalDateTime): List<Question>

    /**
     * 마켓 오픈 후보 조회:
     * - status=BREAK
     * - bettingStartAt <= now(UTC)  // 투표 종료 + 5분 브레이크 경과
     */
    @Query("SELECT q FROM Question q WHERE q.status = 'BREAK' AND q.bettingStartAt <= :time")
    fun findBreakReadyForMarketOpenBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'BETTING' AND q.bettingEndAt < :time")
    fun findBettingExpiredBefore(time: LocalDateTime): List<Question>

    /**
     * 투표 가능 질문 조회: status=VOTING && votingEndAt > now(UTC)
     * null 반환 시 → VOTING_CLOSED (질문 없음 또는 투표 마감)
     */
    @Query("SELECT q FROM Question q WHERE q.id = :id AND q.match IS NULL AND q.status = 'VOTING' AND q.votingEndAt > :now")
    fun findVotableById(@Param("id") id: Long, @Param("now") now: LocalDateTime): Question?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Question q WHERE q.id = :id")
    fun findByIdWithLock(id: Long): Question?

    @Query("SELECT q FROM Question q WHERE q.match IS NOT NULL AND (q.phase IS NULL OR q.phase IN ('UPCOMING', 'VOTING', 'BETTING'))")
    fun findMatchQuestionsForPhaseCheck(): List<Question>

    @Query("SELECT q FROM Question q WHERE q.match.id = :matchId")
    fun findByMatchId(@Param("matchId") matchId: Long): List<Question>

    @Query("SELECT q FROM Question q WHERE q.match IS NOT NULL ORDER BY q.createdAt DESC")
    fun findAllMatchQuestions(): List<Question>

    // 최근 질문 조회 (중복 검사 최적화용)
    fun findByCreatedAtAfter(createdAt: LocalDateTime): List<Question>

    fun countByCreatorMemberIdAndCreatedAtBetween(
        creatorMemberId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Long

    fun countByCreatorMemberId(creatorMemberId: Long): Long

    fun findByCreatorMemberIdOrderByCreatedAtDesc(
        creatorMemberId: Long,
        pageable: Pageable,
    ): Page<Question>

    fun existsByCreatorMemberIdAndStatusIn(
        creatorMemberId: Long,
        statuses: Collection<QuestionStatus>,
    ): Boolean

    /**
     * VOTE_RESULT 질문 중 BETTING 상태이며 votingPhase=VOTING_REVEAL_OPEN인 것:
     * revealWindowEndAt(=bettingEndAt)이 지남 → REVEAL_CLOSED 전환 대상.
     * transitionBettingToSettled() 이전에 실행되어 정산이 즉시 가능하도록 함.
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.voteResultSettlement = true
          AND q.status = 'BETTING'
          AND q.votingPhase = 'VOTING_REVEAL_OPEN'
          AND q.revealWindowEndAt IS NOT NULL
          AND q.revealWindowEndAt <= :now
    """)
    fun findVoteResultQuestionsRevealExpired(@Param("now") now: LocalDateTime): List<Question>

    // Admin 질문 목록 조회 (페이지네이션 + 투표/베팅 수 집계)
    @Query("""
        SELECT q.id as id,
               q.title as title,
               q.category as category,
               q.status as status,
               q.totalBetPool as totalBetPool,
               q.expiredAt as expiredAt,
               q.createdAt as createdAt,
               COUNT(DISTINCT CASE WHEN a.activityType = 'VOTE' THEN a.id ELSE NULL END) as voteCount,
               COUNT(DISTINCT CASE WHEN a.activityType = 'BET' THEN a.id ELSE NULL END) as betCount
        FROM Question q
        LEFT JOIN Activity a ON a.questionId = q.id
        GROUP BY q.id, q.title, q.category, q.status, q.totalBetPool, q.expiredAt, q.createdAt
        ORDER BY q.createdAt DESC
    """)
    fun findAllQuestionsForAdmin(pageable: Pageable): Page<QuestionAdminProjection>

    // Projection interface
    interface QuestionAdminProjection {
        fun getId(): Long
        fun getTitle(): String
        fun getCategory(): String?
        fun getStatus(): QuestionStatus
        fun getTotalBetPool(): Long
        fun getExpiredAt(): LocalDateTime
        fun getCreatedAt(): LocalDateTime
        fun getVoteCount(): Int
        fun getBetCount(): Int
    }
}
