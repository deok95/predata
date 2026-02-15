package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 수수료 풀 엔티티
 * - 질문별 수수료 총액 및 분배 현황 관리
 * - 30% 플랫폼 / 60% 리워드 풀 / 10% 리저브
 */
@Entity
@Table(name = "fee_pools")
data class FeePool(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_pool_id")
    val id: Long? = null,

    @Column(name = "question_id", nullable = false, unique = true)
    val questionId: Long,

    @Column(name = "total_fees", precision = 18, scale = 6, nullable = false)
    var totalFees: BigDecimal = BigDecimal.ZERO,

    @Column(name = "platform_share", precision = 18, scale = 6, nullable = false)
    var platformShare: BigDecimal = BigDecimal.ZERO,

    @Column(name = "reward_pool_share", precision = 18, scale = 6, nullable = false)
    var rewardPoolShare: BigDecimal = BigDecimal.ZERO,

    @Column(name = "reserve_share", precision = 18, scale = 6, nullable = false)
    var reserveShare: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
