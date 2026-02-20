package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.SybilDetectionService
import com.predata.backend.service.SybilSuspiciousAccount
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 시빌 공격 관리자 컨트롤러
 * - 의심스러운 계정 조회
 */
@RestController
@RequestMapping("/api/admin/sybil")
class SybilAdminController(
    private val sybilDetectionService: SybilDetectionService
) {

    /**
     * 특정 질문에 대한 의심스러운 계정 조회
     * GET /api/admin/sybil/report/{questionId}
     */
    @GetMapping("/report/{questionId}")
    fun getSybilReport(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<SybilReportResponse>> {
        val suspiciousAccounts = sybilDetectionService.detectSuspiciousPatterns(questionId)

        return ResponseEntity.ok(
            ApiEnvelope.ok(
                SybilReportResponse(
                    questionId = questionId,
                    suspiciousAccountCount = suspiciousAccounts.size,
                    suspiciousAccounts = suspiciousAccounts
                )
            )
        )
    }
}

data class SybilReportResponse(
    val questionId: Long,
    val suspiciousAccountCount: Int,
    val suspiciousAccounts: List<SybilSuspiciousAccount>
)
