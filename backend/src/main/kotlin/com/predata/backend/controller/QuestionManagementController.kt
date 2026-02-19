package com.predata.backend.controller

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
@CrossOrigin(origins = ["http://localhost:3000"])
class QuestionManagementController(
    private val questionManagementService: QuestionManagementService
) {

    /**
     * 질문 생성 (Duration 기반)
     * POST /api/admin/questions
     * Request: { title, type, category, votingDuration, bettingDuration }
     */
    @PostMapping
    fun createQuestion(@Valid @RequestBody request: com.predata.backend.dto.AdminCreateQuestionRequest): ResponseEntity<QuestionCreationResponse> {
        return try {
            val response = questionManagementService.createQuestionWithDuration(request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                QuestionCreationResponse(
                    success = false,
                    questionId = 0,
                    title = "",
                    category = "",
                    expiredAt = "",
                    message = e.message ?: "Failed to create question."
                )
            )
        }
    }

    /**
     * 질문 생성 (레거시 - expiredAt 기반)
     * POST /api/admin/questions/legacy
     */
    @PostMapping("/legacy")
    fun createQuestionLegacy(@Valid @RequestBody request: CreateQuestionRequest): ResponseEntity<QuestionCreationResponse> {
        return try {
            val response = questionManagementService.createQuestion(request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                QuestionCreationResponse(
                    success = false,
                    questionId = 0,
                    title = "",
                    category = "",
                    expiredAt = "",
                    message = e.message ?: "Failed to create question."
                )
            )
        }
    }

    /**
     * 질문 수정
     * PUT /api/admin/questions/{id}
     */
    @PutMapping("/{id}")
    fun updateQuestion(
        @PathVariable id: Long,
        @RequestBody request: UpdateQuestionRequest
    ): ResponseEntity<QuestionCreationResponse> {
        return try {
            val response = questionManagementService.updateQuestion(id, request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                QuestionCreationResponse(
                    success = false,
                    questionId = 0,
                    title = "",
                    category = "",
                    expiredAt = "",
                    message = e.message ?: "Failed to update question."
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                QuestionCreationResponse(
                    success = false,
                    questionId = 0,
                    title = "",
                    category = "",
                    expiredAt = "",
                    message = e.message ?: "Failed to update question."
                )
            )
        }
    }

    /**
     * 질문 삭제
     * DELETE /api/admin/questions/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteQuestion(@PathVariable id: Long): ResponseEntity<DeleteQuestionResponse> {
        return try {
            val response = questionManagementService.deleteQuestion(id)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                DeleteQuestionResponse(
                    success = false,
                    message = e.message ?: "Failed to delete question."
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                DeleteQuestionResponse(
                    success = false,
                    message = e.message ?: "Failed to delete question."
                )
            )
        }
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
    ): ResponseEntity<Page<QuestionAdminView>> {
        val sort = if (sortDirection.uppercase() == "DESC") {
            Sort.by(Sort.Direction.DESC, sortBy)
        } else {
            Sort.by(Sort.Direction.ASC, sortBy)
        }

        val pageable = PageRequest.of(page, size, sort)
        val questions = questionManagementService.getAllQuestionsForAdmin(pageable)
        return ResponseEntity.ok(questions)
    }

    /**
     * 질문 전체 하드 삭제 (연관 question_id 데이터 포함)
     * DELETE /api/admin/questions/purge-all
     */
    @DeleteMapping("/purge-all")
    fun purgeAllQuestions(): ResponseEntity<PurgeQuestionsResponse> {
        return try {
            ResponseEntity.ok(questionManagementService.purgeAllQuestions())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                PurgeQuestionsResponse(
                    success = false,
                    deletedQuestions = 0,
                    cleanedTables = emptyMap(),
                    message = e.message ?: "Failed to purge all questions."
                )
            )
        }
    }

}
