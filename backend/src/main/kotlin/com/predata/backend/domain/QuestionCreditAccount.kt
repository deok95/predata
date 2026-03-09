package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "question_credit_accounts")
class QuestionCreditAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Column(name = "available_credits", nullable = false)
    var availableCredits: Int = 0,

    @Column(name = "yearly_budget", nullable = false)
    var yearlyBudget: Int = 365,

    @Column(name = "last_reset_at", nullable = false)
    var lastResetAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    /** 스키마 유지용. 앱은 PESSIMISTIC_WRITE 단일 전략 사용. */
    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
