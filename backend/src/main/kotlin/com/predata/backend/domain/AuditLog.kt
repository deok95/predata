package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 감사 로그 엔티티
 * - 시스템 내 중요 작업 기록 (주문, 포지션, 정산, 권한 체크 등)
 */
@Entity
@Table(name = "audit_logs")
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    val id: Long? = null,

    /**
     * 회원 ID (NULL 가능 - 시스템 작업의 경우)
     */
    @Column(name = "member_id")
    val memberId: Long? = null,

    /**
     * 감사 액션
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    val action: AuditAction,

    /**
     * 엔티티 타입 (예: "ORDER", "QUESTION", "POSITION", "ENDPOINT")
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    val entityType: String,

    /**
     * 엔티티 ID
     */
    @Column(name = "entity_id")
    val entityId: Long? = null,

    /**
     * 상세 정보 (예: 에러 메시지, 추가 설명)
     */
    @Column(name = "detail", columnDefinition = "TEXT")
    val detail: String? = null,

    /**
     * IP 주소 (IPv4/IPv6)
     */
    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
