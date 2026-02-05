package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "activities")
data class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    val activityType: ActivityType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val choice: Choice,

    @Column
    val amount: Long = 0, // BET일 때만 사용

    @Column(name = "latency_ms")
    val latencyMs: Int? = null, // 무지성 투표 방지

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null, // IP 추적

    @Column(name = "parent_bet_id")
    val parentBetId: Long? = null, // BET_SELL일 때 원본 베팅 ID 추적

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class ActivityType {
    VOTE, BET, BET_SELL
}

enum class Choice {
    YES, NO
}
