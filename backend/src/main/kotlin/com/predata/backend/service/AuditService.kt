package com.predata.backend.service

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.AuditLog
import com.predata.backend.repository.AuditLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 감사 로그 서비스
 * - 시스템 내 중요 작업을 기록
 * - @Async를 통해 메인 트랜잭션에 영향을 주지 않음
 */
@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {

    /**
     * 감사 로그 기록
     * - 비동기로 실행되어 메인 로직에 영향을 주지 않음
     * - 별도 트랜잭션으로 실행 (REQUIRES_NEW)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        memberId: Long?,
        action: AuditAction,
        entityType: String,
        entityId: Long?,
        detail: String? = null,
        ipAddress: String? = null
    ) {
        try {
            val auditLog = AuditLog(
                memberId = memberId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                detail = detail,
                ipAddress = ipAddress
            )
            auditLogRepository.save(auditLog)
        } catch (e: Exception) {
            // 감사 로그 실패가 메인 로직에 영향을 주지 않도록
            // 에러는 로깅만 하고 던지지 않음
            println("Failed to save audit log: ${e.message}")
        }
    }

    /**
     * 감사 로그 조회
     * - 관리자용 조회 API
     */
    @Transactional(readOnly = true)
    fun queryLogs(
        memberId: Long? = null,
        action: AuditAction? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        pageable: Pageable
    ): Page<AuditLog> {
        // 조건에 따라 다른 쿼리 실행
        return when {
            memberId != null && action != null && from != null && to != null -> {
                auditLogRepository.findByMemberIdAndActionAndCreatedAtBetweenOrderByCreatedAtDesc(
                    memberId, action, from, to, pageable
                )
            }
            memberId != null -> {
                auditLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
            }
            action != null -> {
                auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable)
            }
            from != null && to != null -> {
                auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable)
            }
            else -> {
                // 모든 로그 조회
                auditLogRepository.findAll(pageable)
            }
        }
    }
}
