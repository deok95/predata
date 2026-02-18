package com.predata.backend.dto.amm

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 가격 히스토리 데이터 포인트
 */
data class PricePointResponse(
    val timestamp: LocalDateTime,
    val yesPrice: BigDecimal,
    val noPrice: BigDecimal
)
