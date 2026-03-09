package com.predata.backend.repository

import com.predata.backend.domain.TransactionHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface TransactionHistoryRepository : JpaRepository<TransactionHistory, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<TransactionHistory>
    fun findByMemberIdAndTypeOrderByCreatedAtDesc(memberId: Long, type: String, pageable: Pageable): Page<TransactionHistory>

    @Query(
        """
        SELECT COALESCE(SUM(t.amount), 0)
        FROM TransactionHistory t
        WHERE t.memberId = :memberId
          AND t.type = :type
        """
    )
    fun sumAmountByMemberIdAndType(
        @Param("memberId") memberId: Long,
        @Param("type") type: String,
    ): BigDecimal

    @Query(
        """
        SELECT t.questionId AS questionId, COALESCE(SUM(t.amount), 0) AS totalAmount
        FROM TransactionHistory t
        WHERE t.memberId = :memberId
          AND t.type = :type
          AND t.questionId IN :questionIds
        GROUP BY t.questionId
        """
    )
    fun sumAmountByMemberIdAndTypeGroupedByQuestion(
        @Param("memberId") memberId: Long,
        @Param("type") type: String,
        @Param("questionIds") questionIds: Collection<Long>,
    ): List<QuestionAmountProjection>

    interface QuestionAmountProjection {
        fun getQuestionId(): Long
        fun getTotalAmount(): BigDecimal
    }
}
