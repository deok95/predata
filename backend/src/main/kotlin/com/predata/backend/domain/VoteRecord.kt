package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vote_records")
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
    @Column(nullable = false)
    val choice: Choice,

    @Column(name = "recorded_at", nullable = false)
    val recordedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "tx_hash", nullable = false, length = 64)
    val txHash: String
)
