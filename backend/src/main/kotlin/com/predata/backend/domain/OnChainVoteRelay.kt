package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 온체인 투표 릴레이 큐
 * - vote_records의 투표를 블록체인에 비동기 기록하는 작업 큐
 * - 스케줄러가 PENDING 행을 배치 처리 (FOR UPDATE SKIP LOCKED)
 * - FAILED 행은 retryCount < maxRetries && nextRetryAt 경과 시 재시도
 */
@Entity
@Table(
    name = "onchain_vote_relays",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_ovr_vote_id", columnNames = ["vote_id"])
    ],
    indexes = [
        Index(name = "idx_ovr_status_created", columnList = "status, created_at"),
        Index(name = "idx_ovr_status_retry",   columnList = "status, retry_count, next_retry_at")
    ]
)
class OnChainVoteRelay(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 원본 vote_records.id */
    @Column(name = "vote_id", nullable = false, unique = true)
    val voteId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    val choice: Choice,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OnChainRelayStatus = OnChainRelayStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "max_retries", nullable = false)
    val maxRetries: Int = 8,

    /** 온체인 트랜잭션 해시 (0x 포함 최대 66자) */
    @Column(name = "tx_hash", length = 66)
    var txHash: String? = null,

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,

    /** 다음 재시도 가능 시각 (UTC) */
    @Column(name = "next_retry_at")
    var nextRetryAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
