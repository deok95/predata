package com.predata.backend.repository.market

import com.predata.backend.domain.market.BatchStatus
import com.predata.backend.domain.market.MarketOpenBatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MarketOpenBatchRepository : JpaRepository<MarketOpenBatch, Long> {

    fun findByCutoffSlotUtc(cutoffSlotUtc: LocalDateTime): MarketOpenBatch?

    fun findByStartedAtBetweenOrderByStartedAtDesc(
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<MarketOpenBatch>

    fun findTop1ByStatusOrderByStartedAtDesc(status: BatchStatus): MarketOpenBatch?
}
