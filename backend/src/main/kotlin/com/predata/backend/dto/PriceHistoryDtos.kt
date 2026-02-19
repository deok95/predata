package com.predata.backend.dto

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Price history response DTO
 */
data class PriceHistoryResponse(
    val timestamp: LocalDateTime,
    val midPrice: BigDecimal?,
    val lastTradePrice: BigDecimal?,
    val spread: BigDecimal?
)
