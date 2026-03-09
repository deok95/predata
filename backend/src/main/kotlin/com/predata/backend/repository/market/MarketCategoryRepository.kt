package com.predata.backend.repository.market

import com.predata.backend.domain.market.MarketCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MarketCategoryRepository : JpaRepository<MarketCategory, String> {

    fun findByIsActiveTrueOrderBySortOrderAsc(): List<MarketCategory>
}
