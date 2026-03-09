package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "question_draft_sessions")
class QuestionDraftSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "draft_id", nullable = false, unique = true, length = 36)
    val draftId: String,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /**
     * OPEN 상태일 때만 memberId 값 설정. DB UNIQUE 제약으로 회원당 OPEN draft 1개 보장.
     * CONSUMED/EXPIRED/CANCELLED 전환 시 null 로 변경 → 유니크 슬롯 해제.
     * (SQL 표준: NULL != NULL → 여러 NULL 공존 허용)
     */
    @Column(name = "active_member_id", unique = true)
    var activeMemberId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DraftStatus = DraftStatus.OPEN,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    /** draft-open 시 서버가 발급. submit 시 X-Idempotency-Key 헤더로 검증. */
    @Column(name = "submit_idempotency_key", nullable = false, length = 36)
    val submitIdempotencyKey: String,

    /** 장애 복구용: 커밋 후 크래시 시 재시도에서 기존 questionId 반환 */
    @Column(name = "submitted_question_id")
    var submittedQuestionId: Long? = null,

    @Column(name = "consumed_at")
    var consumedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
)
