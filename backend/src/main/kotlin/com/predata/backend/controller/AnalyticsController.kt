package com.predata.backend.controller

import com.predata.backend.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = ["http://localhost:3000"])
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    /**
     * 페르소나별 투표 분포 조회
     * GET /api/analytics/demographics/{questionId}
     */
    @GetMapping("/demographics/{questionId}")
    fun getVoteDemographics(@PathVariable questionId: Long): ResponseEntity<VoteDemographicsReport> {
        val report = analyticsService.getVoteDemographics(questionId)
        return ResponseEntity.ok(report)
    }

    /**
     * 투표 vs 베팅 괴리율 분석
     * GET /api/analytics/gap-analysis/{questionId}
     */
    @GetMapping("/gap-analysis/{questionId}")
    fun getVoteBetGapAnalysis(@PathVariable questionId: Long): ResponseEntity<VoteBetGapReport> {
        val report = analyticsService.getVoteBetGapAnalysis(questionId)
        return ResponseEntity.ok(report)
    }

    /**
     * 어뷰징 필터링 효과 분석
     * GET /api/analytics/filtering-effect/{questionId}
     */
    @GetMapping("/filtering-effect/{questionId}")
    fun getFilteringEffect(@PathVariable questionId: Long): ResponseEntity<FilteringEffectReport> {
        val report = analyticsService.getFilteringEffectReport(questionId)
        return ResponseEntity.ok(report)
    }

    /**
     * 전체 데이터 품질 대시보드
     * GET /api/analytics/dashboard/{questionId}
     */
    @GetMapping("/dashboard/{questionId}")
    fun getQualityDashboard(@PathVariable questionId: Long): ResponseEntity<QualityDashboard> {
        val dashboard = analyticsService.getQualityDashboard(questionId)
        return ResponseEntity.ok(dashboard)
    }
}
