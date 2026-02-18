package com.predata.backend.repository

import com.predata.backend.domain.Trade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TradeRepository : JpaRepository<Trade, Long> {

    /**
     * 질문의 최근 체결 조회
     */
    fun findTopByQuestionIdOrderByExecutedAtDesc(questionId: Long): Trade?

    /**
     * 질문의 모든 체결 기록 조회
     */
    fun findByQuestionIdOrderByExecutedAtDesc(questionId: Long): List<Trade>

    /**
     * 특정 주문의 체결 기록 조회
     */
    fun findByTakerOrderIdOrMakerOrderId(takerOrderId: Long, makerOrderId: Long): List<Trade>

    /**
     * 특정 기간 이후의 거래 건수 조회 (서킷 브레이커용)
     */
    fun countByQuestionIdAndExecutedAtAfter(questionId: Long, after: LocalDateTime): Long
}
