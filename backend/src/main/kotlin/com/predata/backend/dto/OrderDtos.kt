package com.predata.backend.dto

import com.predata.backend.domain.OrderDirection
import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.OrderStatus
import com.predata.backend.domain.OrderType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Order creation request
 */
data class CreateOrderRequest(
    @field:NotNull(message = "Question ID is required")
    val questionId: Long,

    @field:NotNull(message = "Position (side) is required")
    val side: OrderSide,  // YES or NO

    // For MARKET orders, price is optional (server fills at best price)
    // For LIMIT orders, price is required
    @field:DecimalMin(value = "0.01", message = "Price must be 0.01 or more")
    @field:DecimalMax(value = "0.99", message = "Price must be 0.99 or less")
    val price: BigDecimal? = null,

    @field:NotNull(message = "Amount is required")
    @field:Min(value = 1, message = "Amount must be 1 or more")
    val amount: Long,

    val orderType: OrderType? = null,  // Default: LIMIT (null treated as LIMIT)

    @field:NotNull(message = "Order direction is required")
    val direction: OrderDirection = OrderDirection.BUY  // BUY: buy (deposit USDC), SELL: sell (position collateral)
) {
    @AssertTrue(message = "LIMIT order must have a price.")
    fun isLimitOrderPricePresent(): Boolean {
        val resolvedOrderType = orderType ?: OrderType.LIMIT
        return resolvedOrderType == OrderType.MARKET || price != null
    }
}

/**
 * Order creation response
 */
data class CreateOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val orderId: Long? = null,
    val filledAmount: Long = 0,      // Immediately filled amount
    val remainingAmount: Long = 0     // Amount remaining in order book
)

/**
 * Order book level (aggregated by price)
 */
data class OrderBookLevel(
    val price: BigDecimal,
    val amount: Long,
    val count: Int
)

/**
 * Order book response
 */
data class OrderBookResponse(
    val questionId: Long,
    val bids: List<OrderBookLevel>,   // YES buy (descending order)
    val asks: List<OrderBookLevel>,   // NO buy = YES sell (ascending order)
    val lastPrice: BigDecimal? = null,
    val spread: BigDecimal? = null
)

/**
 * Order cancellation response
 */
data class CancelOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val refundedAmount: Long = 0
)

/**
 * Order detail information
 */
data class OrderResponse(
    val orderId: Long,
    val memberId: Long,
    val questionId: Long,
    val side: OrderSide,
    val direction: OrderDirection,
    val price: BigDecimal,
    val amount: Long,
    val remainingAmount: Long,
    val status: OrderStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Trade information
 */
data class TradeResponse(
    val tradeId: Long,
    val questionId: Long,
    val price: BigDecimal,
    val amount: Long,
    val side: OrderSide,
    val executedAt: LocalDateTime
)
