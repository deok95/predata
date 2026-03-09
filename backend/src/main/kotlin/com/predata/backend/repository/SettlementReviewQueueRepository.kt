package com.predata.backend.repository

import com.predata.backend.domain.SettlementReviewQueue
import com.predata.backend.domain.SettlementReviewStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SettlementReviewQueueRepository : JpaRepository<SettlementReviewQueue, Long> {

    fun findByQuestionId(questionId: Long): SettlementReviewQueue?

    fun findAllByStatus(status: SettlementReviewStatus): List<SettlementReviewQueue>

    /**
     * 재시도 대상 조회: PENDING_RETRY 상태이고 nextRetryAt이 null이거나 현재 시각 이전인 항목.
     * 스케줄러가 소량(최대 50건) 씩 가져와 처리한다.
     */
    @Query("""
        SELECT q FROM SettlementReviewQueue q
        WHERE q.status = 'PENDING_RETRY'
          AND (q.nextRetryAt IS NULL OR q.nextRetryAt <= :now)
        ORDER BY q.nextRetryAt ASC NULLS FIRST
    """)
    fun findDueForRetry(@Param("now") now: LocalDateTime): List<SettlementReviewQueue>
}
