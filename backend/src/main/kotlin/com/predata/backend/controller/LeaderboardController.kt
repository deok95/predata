package com.predata.backend.controller

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
    ): ResponseEntity<List<LeaderboardEntry>> {
        val entries = leaderboardService.getTopPredictors(limit.coerceAtMost(100))
        return ResponseEntity.ok(entries)
    }

    @GetMapping("/member/{memberId}")
    fun getMemberRank(@PathVariable memberId: Long): ResponseEntity<Any> {
        val entry = leaderboardService.getMemberRank(memberId)
        return if (entry != null) ResponseEntity.ok(entry)
        else ResponseEntity.ok(mapOf("rank" to null, "found" to false))
    }
}
