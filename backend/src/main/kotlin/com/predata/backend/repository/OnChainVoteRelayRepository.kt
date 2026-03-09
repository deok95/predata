package com.predata.backend.repository

import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.OnChainVoteRelay
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

@Repository
interface OnChainVoteRelayRepository : JpaRepository<OnChainVoteRelay, Long> {

    /**
     * vote_id로 릴레이 항목 조회 (중복 생성 방지용)
     */
    fun findByVoteId(voteId: Long): OnChainVoteRelay?

    /**
     * 상태별 조회 (모니터링/관리 API용)
     */
    fun findByStatusOrderByCreatedAtAsc(status: OnChainRelayStatus): List<OnChainVoteRelay>
    fun findByStatus(status: OnChainRelayStatus, pageable: Pageable): Page<OnChainVoteRelay>

    /**
     * 재시도 대상 조회: FAILED + 재시도 횟수 여유 + nextRetryAt 경과
     */
    @Query("""
        SELECT r FROM OnChainVoteRelay r
        WHERE r.status = :status
          AND r.retryCount < r.maxRetries
          AND r.nextRetryAt <= :now
        ORDER BY r.nextRetryAt ASC
    """)
    fun findRetryable(
        @Param("status") status: OnChainRelayStatus,
        @Param("now") now: LocalDateTime,
    ): List<OnChainVoteRelay>

    /**
     * 배치 처리용 비관적 락 조회 (FOR UPDATE SKIP LOCKED)
     * - 스케줄러 다중 인스턴스 환경에서 중복 처리 방지
     * - 다른 트랜잭션이 잠금 중인 행은 건너뜀 (SKIP LOCKED)
     * - status는 String으로 전달 (네이티브 쿼리 enum 직렬화 우회)
     */
    @Query(
        value = """
            SELECT * FROM onchain_vote_relays
            WHERE status = :status
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findByStatusForUpdateSkipLocked(
        @Param("status") status: String,
        @Param("limit") limit: Int,
    ): List<OnChainVoteRelay>

    /**
     * 재시도 배치 처리용 비관적 락 조회
     * - FAILED 상태이며 retryCount < maxRetry 이고 nextRetryAt이 경과한 항목
     * - maxRetry는 RelayProperties에서 런타임 주입 (엔티티의 maxRetries 필드 무시)
     */
    @Query(
        value = """
            SELECT * FROM onchain_vote_relays
            WHERE status = 'FAILED'
              AND retry_count < :maxRetry
              AND next_retry_at <= :now
            ORDER BY next_retry_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findRetryableForUpdateSkipLocked(
        @Param("now") now: java.time.LocalDateTime,
        @Param("maxRetry") maxRetry: Int,
        @Param("limit") limit: Int,
    ): List<OnChainVoteRelay>

    /**
     * 운영 정합성 점검용: 상태별 건수 조회
     */
    fun countByStatus(status: OnChainRelayStatus): Long

    /**
     * 보관 기간 초과 터미널 릴레이 삭제 (VoteDataRetentionScheduler 전용)
     * PENDING/SUBMITTED/FAILED 는 미처리 상태이므로 제외
     */
    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            DELETE FROM onchain_vote_relays
            WHERE status IN ('CONFIRMED', 'FAILED_FINAL')
              AND updated_at < :cutoff
        """,
        nativeQuery = true
    )
    fun deleteTerminalOlderThan(@Param("cutoff") cutoff: LocalDateTime): Int
}
