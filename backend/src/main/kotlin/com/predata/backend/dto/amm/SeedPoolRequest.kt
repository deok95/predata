package com.predata.backend.dto.amm

import java.math.BigDecimal

data class SeedPoolRequest(
    val questionId: Long,
    val seedUsdc: BigDecimal,
    val feeRate: BigDecimal = BigDecimal("0.01") // 기본 1%
)

data class SeedPoolResponse(
    val questionId: Long,
    val yesShares: BigDecimal,
    val noShares: BigDecimal,
    val collateralLocked: BigDecimal,
    val feeRate: BigDecimal,
    val k: BigDecimal,
    val currentPrice: PriceSnapshot
)
