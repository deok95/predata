package com.predata.backend.controller

import com.predata.backend.service.*
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
     * 질문 생성
     * POST /api/admin/questions
     */
    @PostMapping
    fun createQuestion(@RequestBody request: CreateQuestionRequest): ResponseEntity<QuestionCreationResponse> {
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
                    message = e.message ?: "질문 생성에 실패했습니다."
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
                    message = e.message ?: "질문 수정에 실패했습니다."
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
                    message = e.message ?: "질문 수정에 실패했습니다."
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
                    message = e.message ?: "질문 삭제에 실패했습니다."
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                DeleteQuestionResponse(
                    success = false,
                    message = e.message ?: "질문 삭제에 실패했습니다."
                )
            )
        }
    }

    /**
     * 모든 질문 조회 (관리자용)
     * GET /api/admin/questions
     */
    @GetMapping
    fun getAllQuestions(): ResponseEntity<List<QuestionAdminView>> {
        val questions = questionManagementService.getAllQuestionsForAdmin()
        return ResponseEntity.ok(questions)
    }
}
