package com.predata.betting.domain

import com.predata.common.domain.ActivityType
import com.predata.common.domain.Choice
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
    @Column(name = "activity_type", length = 20, nullable = false)
    val activityType: ActivityType,

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    val choice: Choice,

    @Column(nullable = false)
    val amount: Long = 0,

    @Column(name = "latency_ms")
    val latencyMs: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
