package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 멱등성 키 엔티티
 * - 중복 요청 방지 (리플레이 공격 차단)
 * - 24시간 TTL
 */
@Entity
@Table(
    name = "idempotency_keys",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_key_member",
            columnNames = ["idempotency_key", "member_id", "endpoint"]
        )
    ],
    indexes = [
        Index(name = "idx_idempotency_key", columnList = "idempotency_key, member_id"),
        Index(name = "idx_idempotency_expires", columnList = "expires_at")
    ]
)
data class IdempotencyKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idempotency_key_id")
    val id: Long? = null,

    @Column(name = "idempotency_key", nullable = false)
    val idempotencyKey: String,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(nullable = false, length = 100)
    val endpoint: String,

    @Column(name = "request_hash", nullable = false, length = 64)
    val requestHash: String,

    @Column(name = "response_body", columnDefinition = "TEXT")
    val responseBody: String,

    @Column(name = "response_status", nullable = false)
    val responseStatus: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now().plusHours(24)
)
