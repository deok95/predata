package com.predata.backend.dto.amm

import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import java.math.BigDecimal

data class SwapRequest(
    val questionId: Long,
    val action: SwapAction,
    val outcome: ShareOutcome,
    val usdcIn: BigDecimal? = null,      // BUY 시 필수
    val sharesIn: BigDecimal? = null,    // SELL 시 필수
    val minSharesOut: BigDecimal? = null, // BUY 시 슬리피지 보호
    val minUsdcOut: BigDecimal? = null    // SELL 시 슬리피지 보호
)
