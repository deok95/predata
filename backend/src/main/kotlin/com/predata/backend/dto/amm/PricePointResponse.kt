package com.predata.backend.dto.amm

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Price history data point
 */
data class PricePointResponse(
    val timestamp: LocalDateTime,
    val yesPrice: BigDecimal,
    val noPrice: BigDecimal
)
