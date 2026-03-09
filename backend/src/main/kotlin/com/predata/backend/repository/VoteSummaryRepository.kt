package com.predata.backend.repository

import com.predata.backend.domain.VoteSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VoteSummaryRepository : JpaRepository<VoteSummary, Long> {

    /**
     * YES 투표 반영 UPSERT
     * - 행 없음 → INSERT (yes=1, no=0, total=1)
     * - 행 있음 → yes_count + 1, total_count + 1
     * clearAutomatically: UPSERT 후 1차 캐시 무효화 (stale read 방지)
     */
    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO vote_summary (question_id, yes_count, no_count, total_count, updated_at)
            VALUES (:questionId, 1, 0, 1, NOW(6))
            ON DUPLICATE KEY UPDATE
                yes_count   = yes_count + 1,
                total_count = total_count + 1,
                updated_at  = NOW(6)
        """,
        nativeQuery = true
    )
    fun upsertIncrementYes(@Param("questionId") questionId: Long)

    /**
     * NO 투표 반영 UPSERT
     * - 행 없음 → INSERT (yes=0, no=1, total=1)
     * - 행 있음 → no_count + 1, total_count + 1
     * clearAutomatically: UPSERT 후 1차 캐시 무효화 (stale read 방지)
     */
    @Modifying(clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO vote_summary (question_id, yes_count, no_count, total_count, updated_at)
            VALUES (:questionId, 0, 1, 1, NOW(6))
            ON DUPLICATE KEY UPDATE
                no_count    = no_count + 1,
                total_count = total_count + 1,
                updated_at  = NOW(6)
        """,
        nativeQuery = true
    )
    fun upsertIncrementNo(@Param("questionId") questionId: Long)
}
