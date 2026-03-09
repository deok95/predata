package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.sports.scheduler.MatchSyncResult
import com.predata.backend.sports.scheduler.MatchSyncScheduler
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "ops-admin", description = "Live admin APIs")
@RequestMapping("/api/admin/live")
class LiveAdminController(
    private val matchSyncScheduler: MatchSyncScheduler
) {

    /**
     * 1주 내 경기 업서트 + match 기반 question 생성
     * POST /api/admin/live/sync
     */
    @PostMapping("/sync")
    fun sync(): ResponseEntity<ApiEnvelope<MatchSyncResult>> {
        val result = matchSyncScheduler.syncUpcomingMatches()
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }
}

