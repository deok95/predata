package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.VoteRewardDistributionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 리워드 관리자 컨트롤러
 * - 수동 분배
 * - 실패분 재시도
 */
@RestController
@RequestMapping("/api/admin/rewards")
class RewardAdminController(
    private val voteRewardDistributionService: VoteRewardDistributionService
) {

    /**
     * 보상 수동 분배
     * POST /api/admin/rewards/distribute/{questionId}
     */
    @PostMapping("/distribute/{questionId}")
    fun distributeRewards(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        val result = voteRewardDistributionService.distributeRewards(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 실패분 재시도
     * POST /api/admin/rewards/retry/{questionId}
     */
    @PostMapping("/retry/{questionId}")
    fun retryFailedDistributions(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        val result = voteRewardDistributionService.retryFailedDistributions(questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }
}
