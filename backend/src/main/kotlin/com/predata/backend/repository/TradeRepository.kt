package com.predata.backend.repository

import com.predata.backend.domain.Trade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

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
    fun findByBuyOrderIdOrSellOrderId(buyOrderId: Long, sellOrderId: Long): List<Trade>
}
