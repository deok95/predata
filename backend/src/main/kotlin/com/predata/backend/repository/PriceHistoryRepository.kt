package com.predata.backend.repository

import com.predata.backend.domain.PriceHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PriceHistoryRepository : JpaRepository<PriceHistory, Long> {

    /**
     * 특정 질문의 최근 가격 이력 조회
     */
    fun findTopByQuestionIdOrderByTimestampDesc(questionId: Long): PriceHistory?

    /**
     * 특정 질문의 가격 이력 조회 (최신순)
     */
    fun findByQuestionIdOrderByTimestampDesc(questionId: Long): List<PriceHistory>

    /**
     * 특정 질문의 가격 이력 조회 (제한)
     */
    @Query("SELECT p FROM PriceHistory p WHERE p.questionId = :questionId ORDER BY p.timestamp DESC LIMIT :limit")
    fun findRecentByQuestionId(questionId: Long, limit: Int): List<PriceHistory>
}
