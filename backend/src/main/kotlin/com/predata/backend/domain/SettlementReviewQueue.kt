package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "settlement_review_queue",
    uniqueConstraints = [UniqueConstraint(name = "uq_srq_question", columnNames = ["question_id"])],
    indexes = [Index(name = "idx_srq_status_retry", columnList = "status,next_retry_at")]
)
class SettlementReviewQueue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: SettlementReviewStatus = SettlementReviewStatus.PENDING_RETRY,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    var reasonCode: SettlementReviewReasonCode,

    @Column(columnDefinition = "TEXT")
    var reasonDetail: String? = null,

    @Column(nullable = false)
    var retryCount: Int = 0,

    @Column(nullable = false)
    val maxRetry: Int = 3,

    @Column
    var nextRetryAt: LocalDateTime? = null,

    @Column
    var lastTriedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
