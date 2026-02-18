package com.predata.backend.dto.amm

import java.math.BigDecimal

data class SwapResponse(
    val sharesAmount: BigDecimal,
    val usdcAmount: BigDecimal,
    val effectivePrice: BigDecimal,
    val fee: BigDecimal,
    val priceBefore: PriceSnapshot,
    val priceAfter: PriceSnapshot,
    val poolState: PoolSnapshot,
    val myShares: MySharesSnapshot
)

data class PriceSnapshot(
    val yes: BigDecimal,
    val no: BigDecimal
)

data class PoolSnapshot(
    val yesShares: BigDecimal,
    val noShares: BigDecimal,
    val collateralLocked: BigDecimal
)

data class MySharesSnapshot(
    val yesShares: BigDecimal,
    val noShares: BigDecimal,
    val yesCostBasis: BigDecimal,
    val noCostBasis: BigDecimal
)
