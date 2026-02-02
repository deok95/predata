package com.predata.member.controller

import com.predata.member.service.TierProgressResponse
import com.predata.member.service.TierService
import com.predata.member.service.TierStatistics
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tiers")
@CrossOrigin(origins = ["http://localhost:3000"])
class TierController(
    private val tierService: TierService
) {

    /**
     * 사용자 티어 진행도 조회
     * GET /api/tiers/progress/{memberId}
     */
    @GetMapping("/progress/{memberId}")
    fun getTierProgress(@PathVariable memberId: Long): ResponseEntity<TierProgressResponse> {
        val progress = tierService.getTierProgress(memberId)
        return ResponseEntity.ok(progress)
    }

    /**
     * 전체 티어 통계
     * GET /api/tiers/statistics
     */
    @GetMapping("/statistics")
    fun getTierStatistics(): ResponseEntity<TierStatistics> {
        val stats = tierService.getTierStatistics()
        return ResponseEntity.ok(stats)
    }
}
