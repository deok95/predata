package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "transaction_history",
    indexes = [
        Index(name = "idx_th_member", columnList = "member_id, created_at"),
        Index(name = "idx_th_member_type", columnList = "member_id, type, created_at")
    ]
)
data class TransactionHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(nullable = false, length = 20)
    val type: String, // DEPOSIT, WITHDRAW, BET, SETTLEMENT, VOTING_PASS

    @Column(nullable = false, precision = 18, scale = 6)
    val amount: BigDecimal, // signed: positive=credit, negative=debit

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 6)
    val balanceAfter: BigDecimal,

    @Column(nullable = false, length = 255)
    val description: String,

    @Column(name = "question_id")
    val questionId: Long? = null,

    @Column(name = "tx_hash", length = 66)
    val txHash: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
