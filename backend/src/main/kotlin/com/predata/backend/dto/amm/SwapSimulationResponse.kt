package com.predata.backend.dto.amm

import java.math.BigDecimal

data class SwapSimulationResponse(
    val sharesOut: BigDecimal?,      // For BUY
    val usdcOut: BigDecimal?,        // For SELL
    val effectivePrice: BigDecimal,
    val slippage: BigDecimal,
    val fee: BigDecimal,
    val minReceived: BigDecimal,     // minSharesOut for BUY, minUsdcOut for SELL
    val priceBefore: PriceSnapshot,
    val priceAfter: PriceSnapshot
)
