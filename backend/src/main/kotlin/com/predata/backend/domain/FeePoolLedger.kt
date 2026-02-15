package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 수수료 풀 원장
 * - 모든 수수료 관련 트랜잭션 기록
 */
@Entity
@Table(name = "fee_pool_ledgers")
data class FeePoolLedger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    val id: Long? = null,

    @Column(name = "fee_pool_id", nullable = false)
    val feePoolId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    val action: FeePoolAction,

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    val amount: BigDecimal,

    @Column(name = "balance", precision = 18, scale = 6, nullable = false)
    val balance: BigDecimal,

    @Column(name = "description", length = 500)
    val description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 수수료 풀 액션 타입
 */
enum class FeePoolAction {
    /** 수수료 수집 */
    FEE_COLLECTED,

    /** 플랫폼 몫 인출 */
    PLATFORM_WITHDRAWN,

    /** 리워드 분배 */
    REWARD_DISTRIBUTED,

    /** 리저브 할당 */
    RESERVE_ALLOCATED
}
