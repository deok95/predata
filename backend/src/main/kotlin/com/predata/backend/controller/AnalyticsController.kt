package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    /**
     * 페르소나별 투표 분포 조회
     * GET /api/analytics/demographics/{questionId}
     */
    @GetMapping("/demographics/{questionId}")
    fun getVoteDemographics(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<VoteDemographicsReport>> {
        val report = analyticsService.getVoteDemographics(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(report))
    }

    /**
     * 투표 vs 베팅 괴리율 분석
     * GET /api/analytics/gap-analysis/{questionId}
     */
    @GetMapping("/gap-analysis/{questionId}")
    fun getVoteBetGapAnalysis(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<VoteBetGapReport>> {
        val report = analyticsService.getVoteBetGapAnalysis(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(report))
    }

    /**
     * 어뷰징 필터링 효과 분석
     * GET /api/analytics/filtering-effect/{questionId}
     */
    @GetMapping("/filtering-effect/{questionId}")
    fun getFilteringEffect(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<FilteringEffectReport>> {
        val report = analyticsService.getFilteringEffectReport(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(report))
    }

    /**
     * 전체 데이터 품질 대시보드
     * GET /api/analytics/dashboard/{questionId}
     */
    @GetMapping("/dashboard/{questionId}")
    fun getQualityDashboard(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<QualityDashboard>> {
        val dashboard = analyticsService.getQualityDashboard(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(dashboard))
    }

    /**
     * 글로벌 통계
     * GET /api/analytics/global/stats
     */
    @GetMapping("/global/stats")
    fun getGlobalStats(): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        val stats = analyticsService.getGlobalStats()
        return ResponseEntity.ok(ApiEnvelope.ok(stats))
    }
}
