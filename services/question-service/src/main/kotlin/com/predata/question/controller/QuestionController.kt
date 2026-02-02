package com.predata.question.controller

import com.predata.common.domain.FinalResult
import com.predata.common.dto.ApiResponse
import com.predata.question.domain.Question
import com.predata.question.service.QuestionService
import com.predata.question.service.CreateQuestionRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = ["http://localhost:3000"])
class QuestionController(
    private val questionService: QuestionService
) {

    @PostMapping
    fun createQuestion(@RequestBody request: CreateQuestionRequest): ApiResponse<Question> {
        val question = questionService.createQuestion(request)
        return ApiResponse(success = true, data = question)
    }

    @GetMapping("/{id}")
    fun getQuestion(@PathVariable id: Long): ApiResponse<Question> {
        val question = questionService.getQuestion(id)
        return ApiResponse(success = true, data = question)
    }

    @GetMapping
    fun getAllQuestions(): ApiResponse<List<Question>> {
        val questions = questionService.getAllQuestions()
        return ApiResponse(success = true, data = questions)
    }

    @PostMapping("/{id}/update-pool")
    fun updateBetPool(
        @PathVariable id: Long,
        @RequestBody request: com.predata.common.dto.UpdatePoolRequest
    ): ApiResponse<Question> {
        val question = questionService.updateBetPool(id, request.choice, request.amount)
        return ApiResponse(success = true, data = question)
    }

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestParam status: String
    ): ApiResponse<Unit> {
        questionService.updateStatus(id, status)
        return ApiResponse(success = true)
    }

    @PutMapping("/{id}/final-result")
    fun updateFinalResult(
        @PathVariable id: Long,
        @RequestBody request: com.predata.common.dto.FinalResultRequest
    ): ApiResponse<Unit> {
        questionService.updateFinalResult(id, request.finalResult)
        return ApiResponse(success = true)
    }

    @PostMapping("/{id}/settle")
    fun settleQuestion(
        @PathVariable id: Long,
        @RequestParam finalResult: FinalResult
    ): ApiResponse<Question> {
        val question = questionService.settleQuestion(id, finalResult)
        return ApiResponse(success = true, data = question)
    }
}
