package com.predata.backend.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 자동 질문 생성 배치 실행 요청
 * - subcategory 미지정 시 첫 번째 subcategory 사용 (스케줄러에서 모든 subcategories 순회)
 * - dryRun=true일 경우 DB 저장 없이 초안만 생성
 */
data class BatchGenerateQuestionsRequest(
    val subcategory: String? = null,
    val targetDate: LocalDate? = null,
    val dryRun: Boolean = false
)

/**
 * 자동 질문 생성 결과 응답
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
 * 생성된 질문 초안 DTO
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
 * 트렌드 기반 입력 신호 DTO
 */
data class TrendSignalDto(
    @field:NotBlank(message = "subcategory는 필수입니다.")
    val subcategory: String,

    @field:NotBlank(message = "keyword는 필수입니다.")
    val keyword: String,

    @field:NotNull(message = "trendScore는 필수입니다.")
    @field:Min(value = 0, message = "trendScore는 0 이상이어야 합니다.")
    @field:Max(value = 100, message = "trendScore는 100 이하여야 합니다.")
    val trendScore: Int,

    val region: String = "US",
    val source: String = "GOOGLE_TRENDS"
)

/**
 * LLM 호출용 정규화 요청 DTO
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
 * LLM 응답(구조화 JSON) DTO
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
 * 배치 검증 결과 DTO
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
 * 운영 재시도/게시 명령 DTO
 */
data class PublishGeneratedBatchRequest(
    @field:NotEmpty(message = "publishDraftIds는 비어 있을 수 없습니다.")
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
