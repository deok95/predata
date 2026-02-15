package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 리워드 분배 엔티티
 * - 각 투표자에 대한 보상 분배 기록
 * - idempotencyKey로 중복 지급 방지
 */
@Entity
@Table(
    name = "reward_distributions",
    indexes = [
        Index(name = "idx_reward_distribution_question_id", columnList = "question_id"),
        Index(name = "idx_reward_distribution_member_id", columnList = "member_id"),
        Index(name = "idx_reward_distribution_status", columnList = "status")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reward_distribution_idempotency", columnNames = ["idempotency_key"])
    ]
)
data class RewardDistribution(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_distribution_id")
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: RewardDistributionStatus = RewardDistributionStatus.PENDING,

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    val idempotencyKey: String,

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    @Column(name = "error_message", length = 1000)
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
)

/**
 * 리워드 분배 상태
 */
enum class RewardDistributionStatus {
    /** 대기 중 */
    PENDING,

    /** 성공 */
    SUCCESS,

    /** 실패 */
    FAILED
}
