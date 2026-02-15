package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.domain.FinalResult
import com.predata.backend.service.SettlementService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/settlements")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class SettlementController(
    private val settlementService: SettlementService
) {

    /**
     * 정산 시작 (PENDING_SETTLEMENT)
     * POST /api/admin/settlements/questions/{id}/settle
     */
    @PostMapping("/questions/{id}/settle")
    fun settleQuestion(
        @PathVariable id: Long,
        @Valid @RequestBody request: SettleQuestionRequest
    ): ResponseEntity<Any> {
        val finalResult = FinalResult.valueOf(request.finalResult)
        val result = settlementService.initiateSettlement(id, finalResult, request.sourceUrl)
        return ResponseEntity.ok(result)
    }

    /**
     * 정산 확정 (SETTLED) — 배당금 분배 실행
     * POST /api/admin/settlements/questions/{id}/finalize
     */
    @PostMapping("/questions/{id}/finalize")
    fun finalizeSettlement(
        @PathVariable id: Long,
        @RequestBody(required = false) request: FinalizeSettlementRequest?
    ): ResponseEntity<Any> {
        val result = settlementService.finalizeSettlement(id, request?.force ?: false)
        return ResponseEntity.ok(result)
    }

    /**
     * 정산 취소 (OPEN으로 복귀)
     * POST /api/admin/settlements/questions/{id}/cancel
     */
    @PostMapping("/questions/{id}/cancel")
    fun cancelSettlement(@PathVariable id: Long): ResponseEntity<Any> {
        val result = settlementService.cancelPendingSettlement(id)
        return ResponseEntity.ok(result)
    }
}
