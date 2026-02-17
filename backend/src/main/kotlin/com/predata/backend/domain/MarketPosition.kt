package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "positions",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["member_id", "question_id", "side"])
    ]
)
data class MarketPosition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val side: OrderSide,  // YES or NO

    @Column(precision = 19, scale = 2, nullable = false)
    var quantity: BigDecimal,  // 포지션 수량

    @Column(name = "reserved_quantity", precision = 19, scale = 2, nullable = false)
    var reservedQuantity: BigDecimal = BigDecimal.ZERO,  // 미체결 SELL 주문으로 예약된 수량

    @Column(name = "avg_price", precision = 4, scale = 2, nullable = false)
    var avgPrice: BigDecimal,  // 평균 매수가

    @Version
    @Column(nullable = false)
    var version: Long = 0,  // 낙관적 락

    @Column(nullable = false)
    var settled: Boolean = false,  // 정산 완료 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
