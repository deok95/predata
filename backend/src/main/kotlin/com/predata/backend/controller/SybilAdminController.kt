package com.predata.backend.controller

import com.predata.backend.service.SybilDetectionService
import com.predata.backend.service.SuspiciousAccount
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
    fun getSybilReport(@PathVariable questionId: Long): ResponseEntity<Map<String, Any>> {
        val suspiciousAccounts = sybilDetectionService.detectSuspiciousPatterns(questionId)

        return ResponseEntity.ok(
            mapOf(
                "questionId" to questionId,
                "suspiciousAccountCount" to suspiciousAccounts.size,
                "suspiciousAccounts" to suspiciousAccounts
            )
        )
    }
}
