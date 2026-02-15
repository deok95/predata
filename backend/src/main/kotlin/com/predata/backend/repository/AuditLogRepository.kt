package com.predata.backend.repository

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    /**
     * 회원별 감사 로그 조회
     */
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<AuditLog>

    /**
     * 액션별 감사 로그 조회
     */
    fun findByActionOrderByCreatedAtDesc(action: AuditAction, pageable: Pageable): Page<AuditLog>

    /**
     * 기간별 감사 로그 조회
     */
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<AuditLog>

    /**
     * 복합 조건 조회 (모든 필터는 optional)
     */
    fun findByMemberIdAndActionAndCreatedAtBetweenOrderByCreatedAtDesc(
        memberId: Long?,
        action: AuditAction?,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<AuditLog>
}
