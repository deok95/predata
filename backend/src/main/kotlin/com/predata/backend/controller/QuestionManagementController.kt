package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/admin/questions")
class QuestionManagementController(
    private val questionManagementService: QuestionManagementService
) {

    /**
     * 질문 생성 (Duration 기반)
     * POST /api/admin/questions
     * Request: { title, type, category, votingDuration, bettingDuration }
     */
    @PostMapping
    fun createQuestion(@Valid @RequestBody request: com.predata.backend.dto.AdminCreateQuestionRequest): ResponseEntity<ApiEnvelope<QuestionCreationResponse>> {
        val response = questionManagementService.createQuestionWithDuration(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(response))
    }

    /**
     * 질문 생성 (레거시 - expiredAt 기반)
     * POST /api/admin/questions/legacy
     */
    @PostMapping("/legacy")
    fun createQuestionLegacy(@Valid @RequestBody request: CreateQuestionRequest): ResponseEntity<ApiEnvelope<QuestionCreationResponse>> {
        val response = questionManagementService.createQuestion(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.ok(response))
    }

    /**
     * 질문 수정
     * PUT /api/admin/questions/{id}
     */
    @PutMapping("/{id}")
    fun updateQuestion(
        @PathVariable id: Long,
        @RequestBody request: UpdateQuestionRequest
    ): ResponseEntity<ApiEnvelope<QuestionCreationResponse>> {
        val response = questionManagementService.updateQuestion(id, request)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 질문 삭제
     * DELETE /api/admin/questions/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteQuestion(@PathVariable id: Long): ResponseEntity<ApiEnvelope<DeleteQuestionResponse>> {
        val response = questionManagementService.deleteQuestion(id)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 모든 질문 조회 (관리자용 - 페이지네이션 지원)
     * GET /api/admin/questions?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    fun getAllQuestions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<ApiEnvelope<Page<QuestionAdminView>>> {
        val sort = if (sortDirection.uppercase() == "DESC") {
            Sort.by(Sort.Direction.DESC, sortBy)
        } else {
            Sort.by(Sort.Direction.ASC, sortBy)
        }

        val pageable = PageRequest.of(page, size, sort)
        val questions = questionManagementService.getAllQuestionsForAdmin(pageable)
        return ResponseEntity.ok(ApiEnvelope.ok(questions))
    }

    /**
     * 질문 전체 하드 삭제 (연관 question_id 데이터 포함)
     * DELETE /api/admin/questions/purge-all
     */
    @DeleteMapping("/purge-all")
    fun purgeAllQuestions(): ResponseEntity<ApiEnvelope<PurgeQuestionsResponse>> {
        return ResponseEntity.ok(ApiEnvelope.ok(questionManagementService.purgeAllQuestions()))
    }

}
