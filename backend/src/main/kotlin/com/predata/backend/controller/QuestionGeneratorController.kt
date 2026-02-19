package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.service.QuestionGeneratorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class QuestionGeneratorController(
    private val questionGeneratorService: QuestionGeneratorService,
    private val autoQuestionGenerationService: com.predata.backend.service.AutoQuestionGenerationService
) {

    /**
     * 질문 생성기 설정 조회
     * GET /api/admin/settings/question-generator
     */
    @GetMapping("/settings/question-generator")
    fun getSettings(): ResponseEntity<QuestionGeneratorSettingsResponse> {
        val settings = questionGeneratorService.getSettings()
        return ResponseEntity.ok(settings)
    }

    /**
     * 질문 생성기 설정 변경
     * PUT /api/admin/settings/question-generator
     */
    @PutMapping("/settings/question-generator")
    fun updateSettings(
        @Valid @RequestBody request: UpdateQuestionGeneratorSettingsRequest
    ): ResponseEntity<QuestionGeneratorSettingsResponse> {
        return try {
            val settings = questionGeneratorService.updateSettings(request)
            ResponseEntity.ok(settings)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 수동으로 질문 1개 생성
     * POST /api/admin/questions/generate
     */
    @PostMapping("/questions/generate")
    fun generateQuestion(): ResponseEntity<QuestionGenerationResponse> {
        val result = autoQuestionGenerationService.generateDailyTrendQuestion()

        return if (result.success) {
            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * 배치 자동 질문 생성 (subcategory당 3문제)
     * POST /api/admin/questions/generate-batch
     */
    @PostMapping("/questions/generate-batch")
    fun generateBatch(
        @RequestBody(required = false) request: BatchGenerateQuestionsRequest?
    ): ResponseEntity<BatchGenerateQuestionsResponse> {
        return try {
            val result = autoQuestionGenerationService.generateBatch(request ?: BatchGenerateQuestionsRequest())
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                BatchGenerateQuestionsResponse(
                    success = false,
                    batchId = "",
                    generatedAt = java.time.LocalDateTime.now(),
                    subcategory = request?.subcategory ?: "",
                    requestedCount = 0,
                    acceptedCount = 0,
                    rejectedCount = 0,
                    opinionCount = 0,
                    verifiableCount = 0,
                    drafts = emptyList(),
                    message = e.message
                )
            )
        }
    }

    /**
     * 배치 상세 조회
     * GET /api/admin/questions/generation-batches/{batchId}
     */
    @GetMapping("/questions/generation-batches/{batchId}")
    fun getBatch(@PathVariable batchId: String): ResponseEntity<BatchGenerateQuestionsResponse> {
        return try {
            ResponseEntity.ok(autoQuestionGenerationService.getBatch(batchId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BatchGenerateQuestionsResponse(
                    success = false,
                    batchId = batchId,
                    generatedAt = java.time.LocalDateTime.now(),
                    subcategory = "",
                    requestedCount = 0,
                    acceptedCount = 0,
                    rejectedCount = 0,
                    opinionCount = 0,
                    verifiableCount = 0,
                    drafts = emptyList(),
                    message = e.message
                )
            )
        }
    }

    /**
     * 초안 게시
     * POST /api/admin/questions/generation-batches/{batchId}/publish
     */
    @PostMapping("/questions/generation-batches/{batchId}/publish")
    fun publishBatch(
        @PathVariable batchId: String,
        @Valid @RequestBody request: PublishGeneratedBatchRequest
    ): ResponseEntity<PublishResultResponse> {
        return try {
            ResponseEntity.ok(autoQuestionGenerationService.publishBatch(batchId, request))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                PublishResultResponse(
                    success = false,
                    batchId = batchId,
                    publishedCount = 0,
                    failedCount = 0,
                    message = e.message
                )
            )
        }
    }

    /**
     * 실패 항목 재시도
     * POST /api/admin/questions/generation-batches/{batchId}/retry
     */
    @PostMapping("/questions/generation-batches/{batchId}/retry")
    fun retryBatch(
        @PathVariable batchId: String,
        @RequestBody(required = false) request: RetryFailedGenerationRequest?
    ): ResponseEntity<BatchGenerateQuestionsResponse> {
        return try {
            ResponseEntity.ok(autoQuestionGenerationService.retryBatch(batchId, request ?: RetryFailedGenerationRequest()))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                BatchGenerateQuestionsResponse(
                    success = false,
                    batchId = batchId,
                    generatedAt = java.time.LocalDateTime.now(),
                    subcategory = "",
                    requestedCount = 0,
                    acceptedCount = 0,
                    rejectedCount = 0,
                    opinionCount = 0,
                    verifiableCount = 0,
                    drafts = emptyList(),
                    message = e.message
                )
            )
        }
    }
}
