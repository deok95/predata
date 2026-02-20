package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.TierProgressResponse
import com.predata.backend.service.TierService
import com.predata.backend.service.TierStatistics
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tiers")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class TierController(
    private val tierService: TierService
) {

    /**
     * 사용자 티어 진행도 조회
     * GET /api/tiers/progress/{memberId}
     */
    @GetMapping("/progress/{memberId}")
    fun getTierProgress(@PathVariable memberId: Long): ResponseEntity<ApiEnvelope<TierProgressResponse>> {
        val progress = tierService.getTierProgress(memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(progress))
    }

    /**
     * 전체 티어 통계
     * GET /api/tiers/statistics
     */
    @GetMapping("/statistics")
    fun getTierStatistics(): ResponseEntity<ApiEnvelope<TierStatistics>> {
        val stats = tierService.getTierStatistics()
        return ResponseEntity.ok(ApiEnvelope.ok(stats))
    }
}
