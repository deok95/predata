package com.predata.backend.controller

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.SettlementReviewStatus
import com.predata.backend.dto.*
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.ErrorCode
import com.predata.backend.service.SettlementAutomationService
import com.predata.backend.service.SettlementReviewQueueService
import com.predata.backend.service.SettlementService
import com.predata.backend.util.authenticatedMemberId
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "settlement-reward", description = "Settlement and reward APIs")
class SettlementController(
    private val settlementService: SettlementService,
    private val settlementAutomationService: SettlementAutomationService,
    private val reviewQueueService: SettlementReviewQueueService,
) {
    private val logger = LoggerFactory.getLogger(SettlementController::class.java)

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
        val result = settlementAutomationService.initiateSettlementAuto(id)
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
        logger.warn("SETTLEMENT_CANCEL_DISABLED questionId={}", id)
        return ResponseEntity.status(HttpStatus.GONE).body(
            ApiEnvelope.error<Any>(
                ErrorCode.SETTLEMENT_CANCELLATION_DISABLED,
                "탈중앙화 정책에 따라 정산 취소는 지원하지 않습니다."
            )
        )
    }

    // ─── 정산 검토 큐 Admin API ───────────────────────────────────────────────

    /**
     * 정산 검토 큐 목록 조회
     * GET /api/admin/settlements/review-queue?status=PENDING_RETRY
     */
    @GetMapping("/api/admin/settlements/review-queue")
    fun listReviewQueue(
        @RequestParam(required = false) status: SettlementReviewStatus?
    ): ResponseEntity<ApiEnvelope<List<SettlementReviewQueueResponse>>> {
        val items = reviewQueueService.findAllByStatus(status)
            .map { SettlementReviewQueueResponse.from(it) }
        return ResponseEntity.ok(ApiEnvelope.ok(items))
    }

    /**
     * 재시도 큐 항목 즉시 수동 재시도
     * POST /api/admin/settlements/review-queue/{id}/retry
     */
    @PostMapping("/api/admin/settlements/review-queue/{id}/retry")
    fun retryReviewQueueEntry(@PathVariable id: Long): ResponseEntity<ApiEnvelope<SettlementReviewQueueResponse>> {
        val entry = reviewQueueService.findById(id)

        if (entry.status == SettlementReviewStatus.RESOLVED) {
            throw ConflictException(
                message = ErrorCode.REVIEW_QUEUE_ALREADY_RESOLVED.message,
                code = ErrorCode.REVIEW_QUEUE_ALREADY_RESOLVED.name
            )
        }

        val result = try {
            settlementAutomationService.autoSettleWithVerification(entry.questionId)
        } catch (e: Exception) {
            logger.error("[ReviewQueue] 수동 재시도 예외 questionId={}: {}", entry.questionId, e.message)
            reviewQueueService.markRetryFailed(entry, e.message ?: "unknown error")
            return ResponseEntity.ok(ApiEnvelope.ok(SettlementReviewQueueResponse.from(entry)))
        }

        if (result != null) {
            reviewQueueService.markResolved(entry.questionId)
            logger.info("[ReviewQueue] 수동 재시도 성공 questionId={}", entry.questionId)
        } else {
            reviewQueueService.markRetryFailed(entry, "autoSettle returned null")
            logger.info("[ReviewQueue] 수동 재시도 조건 미충족 questionId={}", entry.questionId)
        }

        val updated = reviewQueueService.findById(id)
        return ResponseEntity.ok(ApiEnvelope.ok(SettlementReviewQueueResponse.from(updated)))
    }

    /**
     * 수동 확정 처리 (관리자가 직접 결과 입력)
     * POST /api/admin/settlements/review-queue/{id}/resolve
     */
    @PostMapping("/api/admin/settlements/review-queue/{id}/resolve")
    fun resolveManually(
        @PathVariable id: Long,
        @Valid @RequestBody request: ManualResolveRequest,
    ): ResponseEntity<ApiEnvelope<SettlementReviewQueueResponse>> {
        val entry = reviewQueueService.findById(id)

        if (entry.status == SettlementReviewStatus.RESOLVED) {
            throw ConflictException(
                message = ErrorCode.REVIEW_QUEUE_ALREADY_RESOLVED.message,
                code = ErrorCode.REVIEW_QUEUE_ALREADY_RESOLVED.name
            )
        }

        val finalResult = FinalResult.valueOf(request.finalResult)
        settlementService.initiateSettlement(entry.questionId, finalResult, request.sourceUrl)
        settlementService.finalizeSettlement(entry.questionId, skipDeadlineCheck = true)
        reviewQueueService.markResolved(entry.questionId)

        logger.info("[ReviewQueue] 수동 확정 questionId={} result={}", entry.questionId, finalResult)
        val updated = reviewQueueService.findById(id)
        return ResponseEntity.ok(ApiEnvelope.ok(SettlementReviewQueueResponse.from(updated)))
    }
}
