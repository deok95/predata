package com.predata.data.controller

import com.predata.common.client.QuestionClient
import com.predata.common.dto.ApiResponse
import com.predata.data.dto.GlobalStatsDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/analytics/global")
class GlobalStatsController(
    private val questionClient: QuestionClient
) {

    /**
     * 글로벌 통계 조회
     */
    @GetMapping("/stats")
    fun getGlobalStats(): ApiResponse<GlobalStatsDto> {
        val questionsResponse = questionClient.getAllQuestions()
        val questions = questionsResponse.data ?: emptyList()

        val totalQuestions = questions.size
        val totalVolume = questions.sumOf { it.totalBetPool }
        val activeQuestions = questions.count { it.status == "OPEN" }

        return ApiResponse(
            success = true,
            data = GlobalStatsDto(
                totalPredictions = totalQuestions,
                tvl = totalVolume,
                activeUsers = 8400, // TODO: 실제 활성 유저 수 집계 필요
                cumulativeRewards = 1200000, // TODO: Settlement Service에서 집계 필요
                activeMarkets = activeQuestions
            )
        )
    }
}
