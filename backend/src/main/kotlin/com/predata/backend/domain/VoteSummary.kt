package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 질문별 투표 집계 (원자 카운터)
 * - question_id가 PK이자 FK (자동 생성 없음)
 * - 모든 쓰기는 UPSERT(ON DUPLICATE KEY UPDATE)로만 수행 → 원자성 보장
 */
@Entity
@Table(name = "vote_summary")
class VoteSummary(
    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Column(name = "yes_count", nullable = false)
    var yesCount: Long = 0,

    @Column(name = "no_count", nullable = false)
    var noCount: Long = 0,

    @Column(name = "total_count", nullable = false)
    var totalCount: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
