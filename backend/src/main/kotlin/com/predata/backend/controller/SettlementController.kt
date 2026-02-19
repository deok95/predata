package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.domain.FinalResult
import com.predata.backend.service.SettlementService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class SettlementController(
    private val settlementService: SettlementService
) {

    /**
     * 본인의 정산 내역 조회 (JWT 인증 사용)
     * GET /api/settlements/history/me
     */
    @GetMapping("/api/settlements/history/me")
    fun getMySettlementHistory(httpRequest: HttpServletRequest): ResponseEntity<List<com.predata.backend.service.SettlementHistoryItem>> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        val history = settlementService.getSettlementHistory(authenticatedMemberId)
        return ResponseEntity.ok(history)
    }

    /**
     * 정산 시작 (자동 어댑터 기반)
     * POST /api/admin/settlements/questions/{id}/settle-auto
     */
    @PostMapping("/api/admin/settlements/questions/{id}/settle-auto")
    fun settleQuestionAuto(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            val result = settlementService.initiateSettlementAuto(id)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Settlement failed")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (e.message ?: "Settlement failed")))
        }
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
        return ResponseEntity.ok(result)
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
        return ResponseEntity.ok(result)
    }

    /**
     * 정산 취소 (OPEN으로 복귀)
     * POST /api/admin/settlements/questions/{id}/cancel
     */
    @PostMapping("/api/admin/settlements/questions/{id}/cancel")
    fun cancelSettlement(@PathVariable id: Long): ResponseEntity<Any> {
        val result = settlementService.cancelPendingSettlement(id)
        return ResponseEntity.ok(result)
    }
}
