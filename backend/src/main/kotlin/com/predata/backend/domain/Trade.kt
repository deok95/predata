package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "trades")
data class Trade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "buy_order_id", nullable = false)
    val buyOrderId: Long,

    @Column(name = "sell_order_id", nullable = false)
    val sellOrderId: Long,

    @Column(precision = 4, scale = 2, nullable = false)
    val price: BigDecimal,

    @Column(nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val side: OrderSide,

    @Column(name = "executed_at", nullable = false)
    val executedAt: LocalDateTime = LocalDateTime.now()
)
