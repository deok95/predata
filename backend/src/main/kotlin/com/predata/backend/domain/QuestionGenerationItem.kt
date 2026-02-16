package com.predata.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "question_generation_items")
data class QuestionGenerationItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "batch_id", nullable = false, length = 64)
    val batchId: String,

    @Column(name = "draft_id", nullable = false, unique = true, length = 64)
    val draftId: String,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(name = "category", nullable = false, length = 50)
    val category: String,

    @Column(name = "subcategory", nullable = false, length = 50)
    val subcategory: String,

    @Column(name = "market_type", nullable = false, length = 20)
    val marketType: String,

    @Column(name = "question_type", nullable = false, length = 20)
    val questionType: String,

    @Column(name = "vote_result_settlement", nullable = false)
    val voteResultSettlement: Boolean,

    @Column(name = "resolution_rule", nullable = false, columnDefinition = "TEXT")
    val resolutionRule: String,

    @Column(name = "resolution_source", length = 500)
    val resolutionSource: String? = null,

    @Column(name = "resolve_at", nullable = false)
    val resolveAt: LocalDateTime,

    @Column(name = "voting_end_at", nullable = false)
    val votingEndAt: LocalDateTime,

    @Column(name = "break_minutes", nullable = false)
    val breakMinutes: Int,

    @Column(name = "betting_start_at", nullable = false)
    val bettingStartAt: LocalDateTime,

    @Column(name = "betting_end_at", nullable = false)
    val bettingEndAt: LocalDateTime,

    @Column(name = "reveal_start_at", nullable = false)
    val revealStartAt: LocalDateTime,

    @Column(name = "reveal_end_at", nullable = false)
    val revealEndAt: LocalDateTime,

    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    val confidence: java.math.BigDecimal,

    @Column(name = "duplicate_score", nullable = false, precision = 5, scale = 4)
    val duplicateScore: java.math.BigDecimal,

    @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
    val rationale: String,

    @Column(name = "references_json", columnDefinition = "TEXT")
    val referencesJson: String? = null,

    @Column(name = "risk_flags_json", columnDefinition = "TEXT")
    val riskFlagsJson: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    var status: String,

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    var rejectReason: String? = null,

    @Column(name = "published_question_id")
    var publishedQuestionId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
