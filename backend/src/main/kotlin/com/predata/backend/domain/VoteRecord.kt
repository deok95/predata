package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "vote_records",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_vr_member_question", columnNames = ["member_id", "question_id"])
    ],
    indexes = [
        Index(name = "idx_vr_member_id", columnList = "member_id")
    ]
)
data class VoteRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "vote_id", nullable = false)
    val voteId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)  // YES(3) / NO(2)
    val choice: Choice,

    @Column(name = "recorded_at", nullable = false, updatable = false)
    val recordedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "tx_hash", nullable = false, length = 64)
    val txHash: String,
)
