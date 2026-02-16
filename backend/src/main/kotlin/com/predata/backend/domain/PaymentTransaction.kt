package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "payment_transactions",
    indexes = [
        Index(name = "idx_tx_hash", columnList = "tx_hash", unique = true),
        Index(name = "idx_member_payments", columnList = "member_id, created_at")
    ]
)
data class PaymentTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "tx_hash", nullable = false, unique = true, length = 66)
    val txHash: String,

    @Column(nullable = false, precision = 18, scale = 6)
    val amount: BigDecimal,

    @Column(nullable = false, length = 20)
    val type: String, // TICKET_PURCHASE, DEPOSIT

    @Column(nullable = false, length = 20)
    var status: String = "PENDING", // PENDING, CONFIRMED, FAILED

    val quantity: Int? = null, // 티켓 수량 (TICKET_PURCHASE인 경우)

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
