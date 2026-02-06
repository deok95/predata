package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "bet_records")
data class BetRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bet_id", nullable = false)
    val betId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val position: Position,

    @Column(nullable = false)
    val amount: Long,

    @Column(name = "recorded_at", nullable = false)
    val recordedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "tx_hash", nullable = false, length = 64)
    val txHash: String
)

enum class Position {
    LONG, SHORT
}
