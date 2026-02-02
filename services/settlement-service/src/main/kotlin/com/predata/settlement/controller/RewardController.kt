package com.predata.settlement.controller

import com.predata.common.dto.ApiResponse
import com.predata.settlement.service.RewardService
import com.predata.settlement.service.TotalRewardResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = ["http://localhost:3000"])
class RewardController(
    private val rewardService: RewardService
) {

    /**
     * 사용자 누적 보상 조회
     * GET /api/rewards/{memberId}
     */
    @GetMapping("/{memberId}")
    fun getTotalRewards(@PathVariable memberId: Long): ApiResponse<TotalRewardResponse> {
        val rewards = rewardService.getTotalRewards(memberId)
        return ApiResponse(success = true, data = rewards)
    }
}
