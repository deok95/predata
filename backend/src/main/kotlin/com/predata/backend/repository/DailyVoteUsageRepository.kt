package com.predata.backend.repository

import com.predata.backend.domain.DailyVoteUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyVoteUsageRepository : JpaRepository<DailyVoteUsage, Long> {

    /**
     * 특정 회원의 특정 날짜 사용 현황 조회
     */
    fun findByMemberIdAndUsageDate(memberId: Long, usageDate: LocalDate): DailyVoteUsage?

    /**
     * 특정 날짜의 전체 사용 현황 조회 (운영 모니터링용)
     */
    fun findByUsageDateOrderByUsedCountDesc(usageDate: LocalDate): List<DailyVoteUsage>

    /**
     * 보관 기간 초과 레코드 삭제 (VoteDataRetentionScheduler 전용)
     */
    @Modifying(clearAutomatically = true)
    @Query(
        value = "DELETE FROM daily_vote_usage WHERE usage_date < :cutoff",
        nativeQuery = true
    )
    fun deleteOlderThan(@Param("cutoff") cutoff: LocalDate): Int

    /**
     * UPSERT: 해당 날짜 투표 횟수 1 증가
     * - 행 없음 → INSERT (used_count = 1)
     * - 행 있음 → used_count + 1 UPDATE
     * clearAutomatically: UPSERT 후 1차 캐시 무효화 (stale read 방지)
     * UTC_TIMESTAMP(6): DB 세션 타임존 무관 UTC 강제 (MariaDB 전용)
     * H2 테스트: application-test.yml H2 INIT으로 UTC_TIMESTAMP alias 등록
     */
    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO daily_vote_usage (member_id, usage_date, used_count, created_at, updated_at)
            VALUES (:memberId, :usageDate, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                used_count = used_count + 1,
                updated_at = UTC_TIMESTAMP(6)
        """,
        nativeQuery = true
    )
    fun upsertIncrement(
        @Param("memberId") memberId: Long,
        @Param("usageDate") usageDate: LocalDate,
    )
}
