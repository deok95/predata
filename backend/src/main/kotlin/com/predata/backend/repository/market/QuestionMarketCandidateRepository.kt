package com.predata.backend.repository.market

import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.domain.market.QuestionMarketCandidate
import com.predata.backend.domain.market.SelectionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuestionMarketCandidateRepository : JpaRepository<QuestionMarketCandidate, Long> {

    fun findByBatchId(batchId: Long): List<QuestionMarketCandidate>

    fun findByBatchIdAndSelectionStatus(
        batchId: Long,
        selectionStatus: SelectionStatus,
    ): List<QuestionMarketCandidate>

    fun findByBatchIdAndOpenStatus(
        batchId: Long,
        openStatus: OpenStatus,
    ): List<QuestionMarketCandidate>

    fun countByBatchIdAndSelectionStatus(batchId: Long, selectionStatus: SelectionStatus): Int

    fun countByBatchIdAndOpenStatus(batchId: Long, openStatus: OpenStatus): Int

    fun deleteByBatchId(batchId: Long)
}
