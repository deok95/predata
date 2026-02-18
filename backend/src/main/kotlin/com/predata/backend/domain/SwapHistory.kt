package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "swap_history",
    indexes = [
        Index(name = "idx_swap_history_question_created", columnList = "question_id, created_at"),
        Index(name = "idx_swap_history_member_created", columnList = "member_id, created_at")
    ]
)
data class SwapHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "swap_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    val member: Member? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    val question: Question? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 10)
    val action: SwapAction,

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 10)
    val outcome: ShareOutcome,

    @Column(name = "usdc_in", nullable = false, precision = 38, scale = 18)
    val usdcIn: BigDecimal,

    @Column(name = "usdc_out", nullable = false, precision = 38, scale = 18)
    val usdcOut: BigDecimal,

    @Column(name = "shares_in", nullable = false, precision = 38, scale = 18)
    val sharesIn: BigDecimal,

    @Column(name = "shares_out", nullable = false, precision = 38, scale = 18)
    val sharesOut: BigDecimal,

    @Column(name = "fee_usdc", nullable = false, precision = 38, scale = 18)
    val feeUsdc: BigDecimal,

    @Column(name = "price_before_yes", nullable = false, precision = 6, scale = 4)
    val priceBeforeYes: BigDecimal,

    @Column(name = "price_after_yes", nullable = false, precision = 6, scale = 4)
    val priceAfterYes: BigDecimal,

    @Column(name = "yes_before", nullable = false, precision = 38, scale = 18)
    val yesBefore: BigDecimal,

    @Column(name = "no_before", nullable = false, precision = 38, scale = 18)
    val noBefore: BigDecimal,

    @Column(name = "yes_after", nullable = false, precision = 38, scale = 18)
    val yesAfter: BigDecimal,

    @Column(name = "no_after", nullable = false, precision = 38, scale = 18)
    val noAfter: BigDecimal,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class SwapAction {
    BUY,
    SELL
}
