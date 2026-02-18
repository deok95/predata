package com.predata.backend.dto.amm

import com.predata.backend.domain.PoolStatus
import java.math.BigDecimal

data class PoolStateResponse(
    val questionId: Long,
    val status: PoolStatus,
    val yesShares: BigDecimal,
    val noShares: BigDecimal,
    val k: BigDecimal,
    val feeRate: BigDecimal,
    val collateralLocked: BigDecimal,
    val totalVolumeUsdc: BigDecimal,
    val totalFeesUsdc: BigDecimal,
    val currentPrice: PriceSnapshot,
    val version: Long
)
