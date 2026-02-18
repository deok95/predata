package com.predata.backend.dto.amm

import java.math.BigDecimal

data class SwapSimulationResponse(
    val sharesOut: BigDecimal?,      // BUY 시
    val usdcOut: BigDecimal?,        // SELL 시
    val effectivePrice: BigDecimal,
    val slippage: BigDecimal,
    val fee: BigDecimal,
    val minReceived: BigDecimal,     // BUY 시 minSharesOut, SELL 시 minUsdcOut
    val priceBefore: PriceSnapshot,
    val priceAfter: PriceSnapshot
)
