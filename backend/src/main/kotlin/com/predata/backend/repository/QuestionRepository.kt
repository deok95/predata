package com.predata.backend.repository

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
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

    @Query("SELECT q FROM Question q WHERE q.finalResult = 'PENDING' AND q.disputeDeadline < :deadline")
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
}
