package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.service.QuestionGeneratorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = ["http://localhost:3000"])
class QuestionGeneratorController(
    private val questionGeneratorService: QuestionGeneratorService
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
    fun generateQuestion(
        @RequestBody(required = false) request: ManualGenerateQuestionRequest?
    ): ResponseEntity<QuestionGenerationResponse> {
        val result = questionGeneratorService.generateQuestion(request?.category)

        return if (result.success) {
            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}
