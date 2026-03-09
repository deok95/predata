package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "member_wallets")
data class MemberWallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Column(name = "available_balance", precision = 18, scale = 6, nullable = false)
    var availableBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "locked_balance", precision = 18, scale = 6, nullable = false)
    var lockedBalance: BigDecimal = BigDecimal.ZERO,

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0L,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

