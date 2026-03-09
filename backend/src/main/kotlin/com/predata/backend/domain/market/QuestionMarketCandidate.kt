package com.predata.backend.domain.market

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 질문별 선별/오픈 결과
 *
 * - selection_status: SELECTED_TOP3(오픈 대상) | NOT_SELECTED(탈락)
 * - open_status: null(미처리) | OPENED(성공) | OPEN_FAILED(실패)
 * - UNIQUE(batch_id, question_id) → 배치 내 질문 중복 방지
 */
@Entity
@Table(
    name = "question_market_candidates",
    uniqueConstraints = [UniqueConstraint(name = "uk_qmc_batch_question", columnNames = ["batch_id", "question_id"])],
    indexes = [
        Index(name = "idx_qmc_batch_cat_rank", columnList = "batch_id, category, rank_in_category"),
        Index(name = "idx_qmc_open_status",    columnList = "open_status"),
    ]
)
class QuestionMarketCandidate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "batch_id", nullable = false, updatable = false)
    val batchId: Long,

    @Column(name = "question_id", nullable = false, updatable = false)
    val questionId: Long,

    @Column(nullable = false, length = 50, updatable = false)
    val category: String,

    @Column(name = "vote_count", nullable = false, updatable = false)
    val voteCount: Long,

    @Column(name = "rank_in_category", nullable = false, updatable = false)
    val rankInCategory: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_status", nullable = false, length = 20)
    var selectionStatus: SelectionStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_reason", length = 30)
    var selectionReason: SelectionReason? = null,

    @Column(name = "canonical_question_id")
    var canonicalQuestionId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "open_status", length = 20)
    var openStatus: OpenStatus? = null,

    @Column(name = "open_error", columnDefinition = "TEXT")
    var openError: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

enum class SelectionStatus {
    ELIGIBLE,
    SELECTED_TOP3,
    NOT_SELECTED
}

enum class SelectionReason {
    TOP3_SELECTED,
    LOW_RANK,
    DUPLICATE
}

enum class OpenStatus {
    OPENED,
    OPEN_FAILED
}
