package com.predata.backend.controller

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.AuditLog
import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.AuditService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 감사 로그 조회 API (관리자 전용)
 * - AdminAuthInterceptor를 통해 ADMIN 권한 체크
 */
@RestController
@RequestMapping("/api/admin")
class AuditController(
    private val auditService: AuditService
) {

    /**
     * 감사 로그 조회
     * GET /api/admin/audit-logs?memberId=&action=&from=&to=&page=0&size=20
     */
    @GetMapping("/audit-logs")
    fun getAuditLogs(
        @RequestParam(required = false) memberId: Long?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiEnvelope<Page<AuditLogResponse>>> {
        val pageable = PageRequest.of(page, size)
        val logs = auditService.queryLogs(
            memberId = memberId,
            action = action,
            from = from,
            to = to,
            pageable = pageable
        )

        val response = logs.map { log ->
            AuditLogResponse(
                auditLogId = log.id ?: 0L,
                memberId = log.memberId,
                action = log.action.name,
                entityType = log.entityType,
                entityId = log.entityId,
                detail = log.detail,
                ipAddress = log.ipAddress,
                createdAt = log.createdAt
            )
        }

        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = response
            )
        )
    }
}

/**
 * 감사 로그 응답 DTO
 */
data class AuditLogResponse(
    val auditLogId: Long,
    val memberId: Long?,
    val action: String,
    val entityType: String,
    val entityId: Long?,
    val detail: String?,
    val ipAddress: String?,
    val createdAt: LocalDateTime
)
