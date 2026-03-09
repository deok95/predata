package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "treasury_ledgers",
    indexes = [
        Index(name = "idx_treasury_ledger_type_created", columnList = "tx_type, created_at"),
        Index(name = "idx_treasury_ledger_ref", columnList = "reference_type, reference_id"),
    ]
)
data class TreasuryLedger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "treasury_ledger_id")
    val id: Long? = null,

    @Column(name = "tx_type", nullable = false, length = 50)
    val txType: String,

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    val amount: BigDecimal,

    @Column(name = "asset", nullable = false, length = 10)
    val asset: String = "USDC",

    @Column(name = "reference_type", length = 50)
    val referenceType: String? = null,

    @Column(name = "reference_id")
    val referenceId: Long? = null,

    @Column(name = "description", length = 255)
    val description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

