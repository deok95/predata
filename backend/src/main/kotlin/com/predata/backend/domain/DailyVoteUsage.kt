package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 일별 회원 투표 사용 현황
 * - (member_id, usage_date) UNIQUE → UPSERT(ON DUPLICATE KEY UPDATE) 패턴
 * - 일일 투표 한도 체크용 카운터
 */
@Entity
@Table(
    name = "daily_vote_usage",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_dvu_member_date", columnNames = ["member_id", "usage_date"])
    ],
    indexes = [
        Index(name = "idx_dvu_usage_date_count", columnList = "usage_date, used_count")
    ]
)
class DailyVoteUsage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "usage_date", nullable = false, columnDefinition = "DATE")
    val usageDate: LocalDate,

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
