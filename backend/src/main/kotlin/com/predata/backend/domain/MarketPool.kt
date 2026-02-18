package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "market_pools")
data class MarketPool(
    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    val question: Question? = null,

    @Column(name = "yes_shares", nullable = false, precision = 38, scale = 18)
    var yesShares: BigDecimal,

    @Column(name = "no_shares", nullable = false, precision = 38, scale = 18)
    var noShares: BigDecimal,

    @Column(name = "fee_rate", nullable = false, precision = 6, scale = 5)
    var feeRate: BigDecimal,

    @Column(name = "collateral_locked", nullable = false, precision = 38, scale = 18)
    var collateralLocked: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_volume_usdc", nullable = false, precision = 38, scale = 18)
    var totalVolumeUsdc: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_fees_usdc", nullable = false, precision = 38, scale = 18)
    var totalFeesUsdc: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PoolStatus = PoolStatus.ACTIVE,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class PoolStatus {
    ACTIVE,
    PAUSED,
    SETTLED
}
