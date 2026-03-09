package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "question_credit_ledgers")
class QuestionCreditLedger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id")
    val questionId: Long? = null,

    /** 차감: -N, 환불/지급: +N */
    @Column(name = "delta", nullable = false)
    val delta: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    val reason: CreditLedgerReason,

    /** 트랜잭션 후 잔액 스냅샷 — 감사 추적용 */
    @Column(name = "balance_after", nullable = false)
    val balanceAfter: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
