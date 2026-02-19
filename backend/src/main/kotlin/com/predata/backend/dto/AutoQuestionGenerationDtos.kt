package com.predata.backend.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Auto question generation batch execution request
 * - If subcategory is not specified, uses the first subcategory (scheduler iterates through all subcategories)
 * - If dryRun=true, only generates drafts without DB persistence
 */
data class BatchGenerateQuestionsRequest(
    val subcategory: String? = null,
    val targetDate: LocalDate? = null,
    val dryRun: Boolean = false
)

/**
 * Auto question generation result response
 */
data class BatchGenerateQuestionsResponse(
    val success: Boolean,
    val batchId: String,
    val generatedAt: LocalDateTime,
    val subcategory: String,
    val requestedCount: Int,
    val acceptedCount: Int,
    val rejectedCount: Int,
    val opinionCount: Int,
    val verifiableCount: Int,
    val drafts: List<GeneratedQuestionDraftDto>,
    val message: String? = null
)

/**
 * Generated question draft DTO
 */
data class GeneratedQuestionDraftDto(
    val draftId: String,
    val title: String,
    val category: String,
    val subcategory: String,
    val marketType: String,
    val questionType: String,
    val voteResultSettlement: Boolean,
    val resolutionRule: String,
    val resolutionSource: String?,
    val resolveAt: LocalDateTime,
    val votingEndAt: LocalDateTime,
    val breakMinutes: Int,
    val bettingStartAt: LocalDateTime,
    val bettingEndAt: LocalDateTime,
    val revealStartAt: LocalDateTime,
    val revealEndAt: LocalDateTime,
    val duplicateScore: Double,
    val riskFlags: List<String> = emptyList(),
    val status: String
)

/**
 * Trend-based input signal DTO
 */
data class TrendSignalDto(
    @field:NotBlank(message = "subcategory is required.")
    val subcategory: String,

    @field:NotBlank(message = "keyword is required.")
    val keyword: String,

    @field:NotNull(message = "trendScore is required.")
    @field:Min(value = 0, message = "trendScore must be 0 or more.")
    @field:Max(value = 100, message = "trendScore must be 100 or less.")
    val trendScore: Int,

    val region: String = "US",
    val source: String = "GOOGLE_TRENDS"
)

/**
 * Normalized request DTO for LLM invocation
 */
data class LlmQuestionGenerationRequest(
    val subcategory: String,
    val region: String,
    val signals: List<TrendSignalDto>,
    val targetDate: LocalDate,
    val requiredQuestionCount: Int = 3,
    val requiredOpinionCount: Int = 1,
    val minResolveHours: Int = 24,
    val minVotingHours: Int = 24,
    val bettingHours: Int = 24,
    val breakMinutes: Int = 30
)

/**
 * LLM response (structured JSON) DTO
 */
data class LlmQuestionGenerationResponse(
    val batchId: String,
    val model: String,
    val generatedAt: LocalDateTime,
    val subcategory: String,
    val questions: List<LlmGeneratedQuestionDto>
)

data class LlmGeneratedQuestionDto(
    val title: String,
    val marketType: String,
    val questionType: String,
    val voteResultSettlement: Boolean,
    val resolutionRule: String,
    val resolutionSource: String?,
    val resolveAt: LocalDateTime,
    val confidence: Double,
    val rationale: String,
    val references: List<String> = emptyList()
)

/**
 * Batch validation result DTO
 */
data class QuestionBatchValidationResult(
    val batchId: String,
    val passed: Boolean,
    val total: Int,
    val opinionCount: Int,
    val verifiableCount: Int,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Operation retry/publish command DTO
 */
data class PublishGeneratedBatchRequest(
    @field:NotEmpty(message = "publishDraftIds cannot be empty.")
    val publishDraftIds: List<String>
)

data class RetryFailedGenerationRequest(
    val subcategory: String? = null,
    val maxRetryCount: Int = 3,
    val force: Boolean = false
)

data class PublishResultResponse(
    val success: Boolean,
    val batchId: String,
    val publishedCount: Int,
    val failedCount: Int,
    val message: String? = null
)
