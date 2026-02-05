package com.predata.backend.controller

import com.predata.backend.domain.FinalResult
import com.predata.backend.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/questions")
@CrossOrigin(origins = ["http://localhost:3000"])
class QuestionManagementController(
    private val questionManagementService: QuestionManagementService,
    private val settlementService: SettlementService
) {

    /**
     * 질문 생성 (Duration 기반)
     * POST /api/admin/questions
     * Request: { title, type, category, votingDuration, bettingDuration }
     */
    @PostMapping
    fun createQuestion(@RequestBody request: com.predata.backend.dto.AdminCreateQuestionRequest): ResponseEntity<QuestionCreationResponse> {
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
                    message = e.message ?: "질문 생성에 실패했습니다."
                )
            )
        }
    }

    /**
     * 질문 생성 (레거시 - expiredAt 기반)
     * POST /api/admin/questions/legacy
     */
    @PostMapping("/legacy")
    fun createQuestionLegacy(@RequestBody request: CreateQuestionRequest): ResponseEntity<QuestionCreationResponse> {
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

    /**
     * 관리자 결과 입력 및 정산 시작 (VERIFIABLE 타입 질문용)
     * POST /api/admin/questions/{id}/result
     * Request: { result: "YES" or "NO" }
     */
    @PostMapping("/{id}/result")
    fun setQuestionResult(
        @PathVariable id: Long,
        @RequestBody request: com.predata.backend.dto.SetQuestionResultRequest
    ): ResponseEntity<SettlementResult> {
        return try {
            val finalResult = when (request.result.uppercase()) {
                "YES" -> FinalResult.YES
                "NO" -> FinalResult.NO
                else -> throw IllegalArgumentException("결과는 YES 또는 NO만 가능합니다.")
            }

            val settlementResult = settlementService.initiateSettlement(
                questionId = id,
                finalResult = finalResult,
                sourceUrl = "ADMIN_INPUT"
            )

            ResponseEntity.ok(settlementResult)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                SettlementResult(
                    questionId = id,
                    finalResult = "ERROR",
                    totalBets = 0,
                    totalWinners = 0,
                    totalPayout = 0,
                    voterRewards = 0,
                    message = e.message ?: "결과 입력에 실패했습니다."
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                SettlementResult(
                    questionId = id,
                    finalResult = "ERROR",
                    totalBets = 0,
                    totalWinners = 0,
                    totalPayout = 0,
                    voterRewards = 0,
                    message = e.message ?: "결과 입력에 실패했습니다."
                )
            )
        }
    }
}
