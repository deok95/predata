package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.TopQuestionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "market-amm", description = "Top question market APIs")
@RequestMapping("/api/questions")
class TopQuestionController(
    private val topQuestionService: TopQuestionService,
) {
    @GetMapping("/top3")
    fun getTop3Questions(
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "H6") window: String,
        @RequestParam(defaultValue = "3") limit: Int,
    ): ResponseEntity<ApiEnvelope<List<TopQuestionResponse>>> {
        val voteWindow = parseWindow(window)
        val items = topQuestionService.getTopQuestions(category, voteWindow, limit)
            .map {
                TopQuestionResponse(
                    questionId = it.questionId,
                    title = it.title,
                    category = it.category,
                    yesVotes = it.yesVotes,
                    noVotes = it.noVotes,
                    totalVotes = it.totalVotes,
                    yesPrice = it.yesPrice,
                    lastVoteAt = it.lastVoteAt,
                )
            }
        return ResponseEntity.ok(ApiEnvelope.ok(items))
    }

    private fun parseWindow(raw: String): TopQuestionService.VoteWindow {
        return when (raw.uppercase()) {
            "H6" -> TopQuestionService.VoteWindow.H6
            "D1" -> TopQuestionService.VoteWindow.D1
            "D3" -> TopQuestionService.VoteWindow.D3
            else -> TopQuestionService.VoteWindow.H6
        }
    }
}

data class TopQuestionResponse(
    val questionId: Long,
    val title: String,
    val category: String?,
    val yesVotes: Long,
    val noVotes: Long,
    val totalVotes: Long,
    val yesPrice: Double,
    val lastVoteAt: String,
)
