package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "wallet_ledgers",
    indexes = [
        Index(name = "idx_wallet_ledger_member_created", columnList = "member_id, created_at"),
        Index(name = "idx_wallet_ledger_ref", columnList = "reference_type, reference_id"),
    ]
)
data class WalletLedger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    val id: Long? = null,

    @Column(name = "wallet_id", nullable = false)
    val walletId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    val direction: WalletLedgerDirection,

    @Column(name = "tx_type", nullable = false, length = 50)
    val txType: String,

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    val amount: BigDecimal,

    @Column(name = "balance_after", precision = 18, scale = 6, nullable = false)
    val balanceAfter: BigDecimal,

    @Column(name = "locked_balance_after", precision = 18, scale = 6, nullable = false)
    val lockedBalanceAfter: BigDecimal,

    @Column(name = "reference_type", length = 50)
    val referenceType: String? = null,

    @Column(name = "reference_id")
    val referenceId: Long? = null,

    @Column(name = "description", length = 255)
    val description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

enum class WalletLedgerDirection {
    CREDIT, DEBIT
}

