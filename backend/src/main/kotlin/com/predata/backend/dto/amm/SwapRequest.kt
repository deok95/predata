package com.predata.backend.dto.amm

import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import java.math.BigDecimal

data class SwapRequest(
    val questionId: Long,
    val action: SwapAction,
    val outcome: ShareOutcome,
    val usdcIn: BigDecimal? = null,      // Required for BUY
    val sharesIn: BigDecimal? = null,    // Required for SELL
    val minSharesOut: BigDecimal? = null, // Slippage protection for BUY
    val minUsdcOut: BigDecimal? = null    // Slippage protection for SELL
)
