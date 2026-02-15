package com.predata.backend.dto

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 가격 이력 응답 DTO
 */
data class PriceHistoryResponse(
    val timestamp: LocalDateTime,
    val midPrice: BigDecimal?,
    val lastTradePrice: BigDecimal?,
    val spread: BigDecimal?
)
