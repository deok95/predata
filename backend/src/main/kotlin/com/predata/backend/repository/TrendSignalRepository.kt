package com.predata.backend.repository

import com.predata.backend.domain.TrendSignal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TrendSignalRepository : JpaRepository<TrendSignal, Long> {
    fun findBySignalDateAndSubcategoryOrderByTrendScoreDesc(signalDate: LocalDate, subcategory: String): List<TrendSignal>
    fun findBySignalDateAndSubcategoryAndRegionOrderByTrendScoreDesc(signalDate: LocalDate, subcategory: String, region: String): List<TrendSignal>
}
