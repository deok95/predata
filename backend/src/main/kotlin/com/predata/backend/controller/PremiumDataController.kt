package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/premium-data")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class PremiumDataController(
    private val premiumDataService: PremiumDataService
) {

    /**
     * 프리미엄 데이터 미리보기 (최대 10개)
     * POST /api/premium-data/preview
     */
    @PostMapping("/preview")
    fun previewPremiumData(@RequestBody request: PremiumDataRequest): ResponseEntity<ApiEnvelope<PremiumDataResponse>> {
        val preview = premiumDataService.previewPremiumData(request)
        return ResponseEntity.ok(ApiEnvelope.ok(preview))
    }

    /**
     * 프리미엄 데이터 전체 추출
     * POST /api/premium-data/export
     */
    @PostMapping("/export")
    fun exportPremiumData(@RequestBody request: PremiumDataRequest): ResponseEntity<ApiEnvelope<PremiumDataResponse>> {
        val data = premiumDataService.extractPremiumData(request)
        return ResponseEntity.ok(ApiEnvelope.ok(data))
    }

    /**
     * 데이터 품질 요약
     * GET /api/premium-data/quality-summary/{questionId}
     */
    @GetMapping("/quality-summary/{questionId}")
    fun getQualitySummary(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<DataQualitySummary>> {
        val summary = premiumDataService.getDataQualitySummary(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(summary))
    }
}
