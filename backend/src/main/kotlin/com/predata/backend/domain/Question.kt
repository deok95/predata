package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "questions")
data class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(length = 50)
    var category: String? = null,

    @Column(name = "category_weight", precision = 3, scale = 2)
    val categoryWeight: BigDecimal = BigDecimal("1.00"),

    @Column(length = 20)
    var status: String = "OPEN", // OPEN, CLOSED, SETTLED

    @Column(name = "total_bet_pool")
    var totalBetPool: Long = 0,

    @Column(name = "yes_bet_pool")
    var yesBetPool: Long = 0,

    @Column(name = "no_bet_pool")
    var noBetPool: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "final_result")
    var finalResult: FinalResult = FinalResult.PENDING,

    @Column(name = "source_url", columnDefinition = "TEXT")
    var sourceUrl: String? = null,

    @Column(name = "dispute_deadline")
    var disputeDeadline: LocalDateTime? = null,

    @Column(name = "expired_at", nullable = false)
    val expiredAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class FinalResult {
    YES, NO, PENDING
}
