package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    val orderType: OrderType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val side: OrderSide,

    @Column(precision = 4, scale = 2, nullable = false)
    val price: BigDecimal,

    @Column(nullable = false)
    val amount: Long,

    @Column(name = "remaining_amount", nullable = false)
    var remainingAmount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.OPEN,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class OrderType {
    BUY, SELL
}

enum class OrderSide {
    YES, NO
}

enum class OrderStatus {
    OPEN, FILLED, PARTIAL, CANCELLED
}
