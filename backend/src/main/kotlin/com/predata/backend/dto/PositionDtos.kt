package com.predata.backend.dto

import com.predata.backend.domain.OrderSide
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Position response DTO (includes PnL)
 */
data class PositionResponse(
    val positionId: Long,
    val questionId: Long,
    val questionTitle: String,
    val side: OrderSide,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentMidPrice: BigDecimal?,
    val unrealizedPnL: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
