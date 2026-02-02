package com.predata.betting.controller

import com.predata.betting.domain.Activity
import com.predata.betting.service.BetService
import com.predata.betting.service.PlaceBetRequest
import com.predata.common.domain.ActivityType
import com.predata.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:3000"])
class BetController(
    private val betService: BetService
) {

    @PostMapping("/bets")
    fun placeBet(@RequestBody request: PlaceBetRequest): ApiResponse<Activity> {
        val activity = betService.placeBet(request)
        return ApiResponse(success = true, data = activity)
    }

    @GetMapping("/activities/question/{questionId}")
    fun getActivitiesByQuestion(
        @PathVariable questionId: Long,
        @RequestParam(required = false) type: ActivityType?
    ): ApiResponse<List<Activity>> {
        val activities = betService.getActivitiesByQuestion(questionId, type)
        return ApiResponse(success = true, data = activities)
    }

    @GetMapping("/activities/member/{memberId}")
    fun getActivitiesByMember(@PathVariable memberId: Long): ApiResponse<List<Activity>> {
        val activities = betService.getActivitiesByMember(memberId)
        return ApiResponse(success = true, data = activities)
    }
}
