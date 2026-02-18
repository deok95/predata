package com.predata.backend.domain

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_shares",
    indexes = [
        Index(name = "idx_user_shares_question_outcome", columnList = "question_id, outcome"),
        Index(name = "idx_user_shares_member", columnList = "member_id")
    ]
)
@IdClass(UserSharesId::class)
data class UserShares(
    @Id
    @Column(name = "member_id")
    val memberId: Long,

    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 10)
    val outcome: ShareOutcome,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    val member: Member? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    val question: Question? = null,

    @Column(name = "shares", nullable = false, precision = 38, scale = 18)
    var shares: BigDecimal = BigDecimal.ZERO,

    @Column(name = "cost_basis_usdc", nullable = false, precision = 38, scale = 18)
    var costBasisUsdc: BigDecimal = BigDecimal.ZERO,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class UserSharesId(
    val memberId: Long = 0,
    val questionId: Long = 0,
    val outcome: ShareOutcome = ShareOutcome.YES
) : Serializable

enum class ShareOutcome {
    YES,
    NO
}
