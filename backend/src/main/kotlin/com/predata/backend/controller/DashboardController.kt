package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.VotingDashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 대시보드 컨트롤러
 * - 전체 대시보드 조회
 * - 질문별 상태 조회
 */
@RestController
@RequestMapping("/api/admin/dashboard")
class DashboardController(
    private val votingDashboardService: VotingDashboardService
) {

    /**
     * 전체 투표 시스템 대시보드
     * GET /api/admin/dashboard/voting
     */
    @GetMapping("/voting")
    fun getVotingDashboard(): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        val dashboard = votingDashboardService.getDashboard()
        return ResponseEntity.ok(ApiEnvelope.ok(dashboard))
    }

    /**
     * 특정 질문의 상태 조회
     * GET /api/admin/dashboard/voting/{questionId}
     */
    @GetMapping("/voting/{questionId}")
    fun getQuestionHealth(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        val health = votingDashboardService.getQuestionHealth(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(health))
    }
}
