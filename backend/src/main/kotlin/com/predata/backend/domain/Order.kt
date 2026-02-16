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
    LIMIT,   // 지정가 주문 (오더북 적재)
    MARKET   // 시장가 IOC 주문 (즉시 체결, 미체결분 취소)
}

enum class OrderSide {
    YES, NO
}

enum class OrderStatus {
    OPEN, FILLED, PARTIAL, CANCELLED
}
