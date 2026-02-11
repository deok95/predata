package com.predata.backend.repository

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType
import java.time.LocalDateTime

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {

    fun findByStatus(status: QuestionStatus): List<Question>

    // Legacy queries (kept for compatibility)
    @Query("SELECT q FROM Question q WHERE q.status = 'OPEN' AND q.expiredAt < :time")
    fun findOpenExpiredBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'SETTLED' AND q.finalResult != 'PENDING' AND q.disputeDeadline IS NOT NULL AND q.disputeDeadline < :deadline")
    fun findPendingSettlementPastDeadline(deadline: LocalDateTime): List<Question>

    // New status transition queries
    @Query("SELECT q FROM Question q WHERE q.status = 'VOTING' AND q.votingEndAt < :time")
    fun findVotingExpiredBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'BREAK' AND q.bettingStartAt < :time")
    fun findBreakExpiredBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'BETTING' AND q.bettingEndAt < :time")
    fun findBettingExpiredBefore(time: LocalDateTime): List<Question>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Question q WHERE q.id = :id")
    fun findByIdWithLock(id: Long): Question?

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
