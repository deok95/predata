package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.RewardService
import com.predata.backend.service.TotalRewardResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "settlement-reward", description = "Reward APIs")
@RequestMapping("/api/rewards")
class RewardController(
    private val rewardService: RewardService
) {

    /**
     * 사용자 누적 보상 조회
     * GET /api/rewards/{memberId}
     */
    @GetMapping("/{memberId}")
    fun getTotalRewards(@PathVariable memberId: Long): ResponseEntity<ApiEnvelope<TotalRewardResponse>> {
        val rewards = rewardService.getTotalRewards(memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(rewards))
    }
}
