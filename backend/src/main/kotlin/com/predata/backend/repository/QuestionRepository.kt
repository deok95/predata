package com.predata.backend.repository

import com.predata.backend.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType
import java.time.LocalDateTime

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {

    fun findByStatus(status: String): List<Question>

    @Query("SELECT q FROM Question q WHERE q.status = 'OPEN' AND q.expiredAt < :time")
    fun findOpenExpiredBefore(time: LocalDateTime): List<Question>

    @Query("SELECT q FROM Question q WHERE q.finalResult = 'PENDING' AND q.disputeDeadline < :deadline")
    fun findPendingSettlementPastDeadline(deadline: LocalDateTime): List<Question>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Question q WHERE q.id = :id")
    fun findByIdWithLock(id: Long): Question?
}
