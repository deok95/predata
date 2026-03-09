package com.predata.backend.domain.market

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 마켓 오픈 배치 실행 단위
 *
 * 스케줄러가 5분 주기로 실행하며, cutoff_slot_utc 기준 UNIQUE → 중복 실행 방지.
 * 상태 FSM: PENDING → SELECTED → OPENING → COMPLETED | PARTIAL_FAILED | FAILED
 */
@Entity
@Table(
    name = "market_open_batches",
    uniqueConstraints = [UniqueConstraint(name = "uk_mob_cutoff_slot", columnNames = ["cutoff_slot_utc"])]
)
class MarketOpenBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 배치 기준 UTC 시각 슬롯 (분 단위 truncate) */
    @Column(name = "cutoff_slot_utc", nullable = false, updatable = false)
    val cutoffSlotUtc: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BatchStatus = BatchStatus.PENDING,

    @Column(name = "started_at", nullable = false, updatable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Column(name = "total_candidates", nullable = false)
    var totalCandidates: Int = 0,

    @Column(name = "selected_count", nullable = false)
    var selectedCount: Int = 0,

    @Column(name = "opened_count", nullable = false)
    var openedCount: Int = 0,

    @Column(name = "failed_count", nullable = false)
    var failedCount: Int = 0,

    @Column(name = "error_summary", columnDefinition = "TEXT")
    var errorSummary: String? = null,
)

enum class BatchStatus {
    PENDING,
    SELECTED,
    OPENING,
    COMPLETED,
    PARTIAL_FAILED,
    FAILED
}
