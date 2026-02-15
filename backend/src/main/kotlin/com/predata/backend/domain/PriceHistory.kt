package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "price_history")
data class PriceHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_history_id")
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "mid_price", precision = 4, scale = 2)
    val midPrice: BigDecimal?,  // (bestBid + bestAsk) / 2

    @Column(name = "last_trade_price", precision = 4, scale = 2)
    val lastTradePrice: BigDecimal?,  // 최근 체결가

    @Column(precision = 4, scale = 2)
    val spread: BigDecimal?,  // bestAsk - bestBid

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
