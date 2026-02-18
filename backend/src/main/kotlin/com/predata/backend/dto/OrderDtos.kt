package com.predata.backend.dto

import com.predata.backend.domain.OrderDirection
import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.OrderStatus
import com.predata.backend.domain.OrderType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 생성 요청
 */
data class CreateOrderRequest(
    @field:NotNull(message = "질문 ID는 필수입니다")
    val questionId: Long,

    @field:NotNull(message = "포지션(side)은 필수입니다")
    val side: OrderSide,  // YES or NO

    // MARKET 주문일 경우 price는 optional (서버가 최적가로 체결)
    // LIMIT 주문일 경우 price는 필수
    @field:DecimalMin(value = "0.01", message = "가격은 0.01 이상이어야 합니다")
    @field:DecimalMax(value = "0.99", message = "가격은 0.99 이하이어야 합니다")
    val price: BigDecimal? = null,

    @field:NotNull(message = "수량은 필수입니다")
    @field:Min(value = 1, message = "수량은 1 이상이어야 합니다")
    val amount: Long,

    val orderType: OrderType? = null,  // 기본값: LIMIT (null이면 LIMIT으로 처리)

    @field:NotNull(message = "주문 방향은 필수입니다")
    val direction: OrderDirection = OrderDirection.BUY  // BUY: 매수(USDC 예치), SELL: 매도(포지션 담보)
) {
    @AssertTrue(message = "지정가(LIMIT) 주문은 가격을 입력해야 합니다.")
    fun isLimitOrderPricePresent(): Boolean {
        val resolvedOrderType = orderType ?: OrderType.LIMIT
        return resolvedOrderType == OrderType.MARKET || price != null
    }
}

/**
 * 주문 생성 응답
 */
data class CreateOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val orderId: Long? = null,
    val filledAmount: Long = 0,      // 즉시 체결된 수량
    val remainingAmount: Long = 0     // 오더북에 남은 수량
)

/**
 * 오더북 레벨 (가격별 집계)
 */
data class OrderBookLevel(
    val price: BigDecimal,
    val amount: Long,
    val count: Int
)

/**
 * 오더북 응답
 */
data class OrderBookResponse(
    val questionId: Long,
    val bids: List<OrderBookLevel>,   // YES 매수 (내림차순)
    val asks: List<OrderBookLevel>,   // NO 매수 = YES 매도 (오름차순)
    val lastPrice: BigDecimal? = null,
    val spread: BigDecimal? = null
)

/**
 * 주문 취소 응답
 */
data class CancelOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val refundedAmount: Long = 0
)

/**
 * 주문 상세 정보
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
 * 체결 정보
 */
data class TradeResponse(
    val tradeId: Long,
    val questionId: Long,
    val price: BigDecimal,
    val amount: Long,
    val side: OrderSide,
    val executedAt: LocalDateTime
)
