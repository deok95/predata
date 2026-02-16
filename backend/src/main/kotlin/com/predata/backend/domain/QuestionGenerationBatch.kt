package com.predata.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "question_generation_batches")
data class QuestionGenerationBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "batch_id", nullable = false, unique = true, length = 64)
    val batchId: String,

    @Column(name = "subcategory", nullable = false, length = 50)
    val subcategory: String,

    @Column(name = "target_date", nullable = false)
    val targetDate: LocalDate,

    @Column(name = "status", nullable = false, length = 20)
    var status: String,

    @Column(name = "requested_count", nullable = false)
    var requestedCount: Int = 3,

    @Column(name = "accepted_count", nullable = false)
    var acceptedCount: Int = 0,

    @Column(name = "rejected_count", nullable = false)
    var rejectedCount: Int = 0,

    @Column(name = "dry_run", nullable = false)
    val dryRun: Boolean = false,

    @Column(name = "message", columnDefinition = "TEXT")
    var message: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
