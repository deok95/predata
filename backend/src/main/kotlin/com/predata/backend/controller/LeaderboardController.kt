package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.LeaderboardEntry
import com.predata.backend.service.LeaderboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leaderboard")
class LeaderboardController(
    private val leaderboardService: LeaderboardService
) {

    @GetMapping("/top")
    fun getTopLeaderboard(
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<ApiEnvelope<List<LeaderboardEntry>>> {
        val entries = leaderboardService.getTopPredictors(limit.coerceAtMost(100))
        return ResponseEntity.ok(ApiEnvelope.ok(entries))
    }

    @GetMapping("/member/{memberId}")
    fun getMemberRank(@PathVariable memberId: Long): ResponseEntity<ApiEnvelope<MemberRankResponse>> {
        val entry = leaderboardService.getMemberRank(memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(MemberRankResponse(entry = entry, found = entry != null)))
    }
}

data class MemberRankResponse(
    val entry: LeaderboardEntry?,
    val found: Boolean
)
