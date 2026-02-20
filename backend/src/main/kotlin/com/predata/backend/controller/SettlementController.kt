package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.domain.FinalResult
import com.predata.backend.service.SettlementService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SettlementController(
    private val settlementService: SettlementService
) {

    /**
     * 본인의 정산 내역 조회 (JWT 인증 사용)
     * GET /api/settlements/history/me
     */
    @GetMapping("/api/settlements/history/me")
    fun getMySettlementHistory(httpRequest: HttpServletRequest): ResponseEntity<ApiEnvelope<List<com.predata.backend.service.SettlementHistoryItem>>> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        val history = settlementService.getSettlementHistory(authenticatedMemberId)
        return ResponseEntity.ok(ApiEnvelope.ok(history))
    }

    /**
     * 정산 시작 (자동 어댑터 기반)
     * POST /api/admin/settlements/questions/{id}/settle-auto
     */
    @PostMapping("/api/admin/settlements/questions/{id}/settle-auto")
    fun settleQuestionAuto(@PathVariable id: Long): ResponseEntity<Any> {
        val result = settlementService.initiateSettlementAuto(id)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 정산 시작 (수동 - PENDING_SETTLEMENT)
     * POST /api/admin/settlements/questions/{id}/settle
     */
    @PostMapping("/api/admin/settlements/questions/{id}/settle")
    fun settleQuestion(
        @PathVariable id: Long,
        @Valid @RequestBody request: SettleQuestionRequest
    ): ResponseEntity<Any> {
        val finalResult = FinalResult.valueOf(request.finalResult)
        val result = settlementService.initiateSettlement(id, finalResult, request.sourceUrl)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 정산 확정 (SETTLED) — 배당금 분배 실행
     * POST /api/admin/settlements/questions/{id}/finalize
     */
    @PostMapping("/api/admin/settlements/questions/{id}/finalize")
    fun finalizeSettlement(
        @PathVariable id: Long,
        @RequestBody(required = false) request: FinalizeSettlementRequest?
    ): ResponseEntity<Any> {
        val result = settlementService.finalizeSettlement(id, request?.force ?: false)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 정산 취소 (OPEN으로 복귀)
     * POST /api/admin/settlements/questions/{id}/cancel
     */
    @PostMapping("/api/admin/settlements/questions/{id}/cancel")
    fun cancelSettlement(@PathVariable id: Long): ResponseEntity<Any> {
        val result = settlementService.cancelPendingSettlement(id)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }
}
