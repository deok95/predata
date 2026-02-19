package com.predata.backend.dto.amm

import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import java.math.BigDecimal
import java.time.LocalDateTime

data class SwapHistoryResponse(
    val swapId: Long,
    val memberId: Long,
    val memberEmail: String? = null,
    val action: SwapAction,
    val outcome: ShareOutcome,
    val usdcAmount: BigDecimal,
    val sharesAmount: BigDecimal,
    val effectivePrice: BigDecimal,
    val feeUsdc: BigDecimal,
    val priceAfterYes: BigDecimal,
    val createdAt: LocalDateTime
)
