package com.predata.backend.service

import com.predata.backend.domain.*
import com.predata.backend.dto.*
import com.predata.backend.repository.*
import jakarta.persistence.OptimisticLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderMatchingService(
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository,
    private val memberRepository: MemberRepository,
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val transactionHistoryService: TransactionHistoryService,
    private val bettingSuspensionService: BettingSuspensionService,
    private val positionService: PositionService,
    private val orderBookService: OrderBookService,
    private val priceHistoryRepository: com.predata.backend.repository.PriceHistoryRepository,
    private val auditService: AuditService,
    private val riskGuardService: RiskGuardService,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(OrderMatchingService::class.java)

    fun <T> retryOnOptimisticLock(maxRetries: Int = 3, action: () -> T): T {
        repeat(maxRetries) { attempt ->
            try {
                return action()
            } catch (e: OptimisticLockException) {
                if (attempt == maxRetries - 1) throw e
                Thread.sleep(50L * (attempt + 1))
            } catch (e: ObjectOptimisticLockingFailureException) {
                if (attempt == maxRetries - 1) throw e
                Thread.sleep(50L * (attempt + 1))
            }
        }
        throw IllegalStateException("Retry exhausted")
    }

    /**
     * Limit Order 생성 및 매칭
     *
     * 예시: YES를 0.60에 100P 매수
     * 1. 0.40 이하의 NO 매수 주문과 매칭 (YES 0.60 = NO 0.40 역가격)
     * 2. 매칭되지 않은 수량은 오더북에 추가
     */
    fun createOrder(memberId: Long, request: CreateOrderRequest): CreateOrderResponse {
        return retryOnOptimisticLock {
            transactionTemplate.execute {
                createOrderInternal(memberId, request)
            } ?: throw IllegalStateException("Transaction returned null")
        }
    }

    private fun createOrderInternal(memberId: Long, request: CreateOrderRequest): CreateOrderResponse {
        // 0. SELL 주문 임시 비활성화 (현재 YES BUY ↔ NO BUY 매칭 구조에서 SELL 정상 체결 불가)
        if (request.direction == OrderDirection.SELL) {
            return CreateOrderResponse(
                success = false,
                message = "Sell feature is under development. Currently only BUY orders are available."
            )
        }

        // 1. 베팅 일시 중지 체크 (쿨다운)
        val suspensionStatus = bettingSuspensionService.isBettingSuspendedByQuestionId(request.questionId)
        if (suspensionStatus.suspended) {
            return CreateOrderResponse(
                success = false,
                message = "⚠️ Betting is temporarily suspended after a goal. It will resume in ${suspensionStatus.remainingSeconds} seconds."
            )
        }

        // 2. 회원 및 잔액 확인 (row lock으로 잔고 변경 정합성 확보)
        val member = memberRepository.findByIdForUpdate(memberId) ?: return CreateOrderResponse(
                success = false,
                message = "Member not found."
            )

        if (member.isBanned) {
            return CreateOrderResponse(
                success = false,
                message = "Account has been suspended."
            )
        }

        // 3. 주문 타입 결정
        val orderType = request.orderType ?: OrderType.LIMIT  // 기본값: LIMIT

        // 4. LIMIT 주문일 경우 price 필수 검증
        if (orderType == OrderType.LIMIT && request.price == null) {
            return CreateOrderResponse(
                success = false,
                message = "Price is required for limit orders."
            )
        }

        // 2. 질문 상태 확인
        val question = questionRepository.findByIdWithLock(request.questionId)
            ?: return CreateOrderResponse(
                success = false,
                message = "Question not found."
            )

        if (question.status != QuestionStatus.BETTING) {
            return CreateOrderResponse(
                success = false,
                message = "Not in betting period. (Current: ${question.status})"
            )
        }

        if (question.expiredAt.isBefore(LocalDateTime.now())) {
            return CreateOrderResponse(
                success = false,
                message = "Betting period has expired."
            )
        }

        // 리스크 가드: 포지션 한도 체크
        val positionCheck = riskGuardService.checkPositionLimit(
            memberId = memberId,
            questionId = request.questionId,
            additionalQty = BigDecimal(request.amount)
        )
        if (!positionCheck.passed) {
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.RISK_LIMIT_EXCEEDED,
                entityType = "ORDER",
                entityId = null,
                detail = positionCheck.message
            )
            return CreateOrderResponse(
                success = false,
                message = positionCheck.message!!
            )
        }

        // 리스크 가드: 서킷 브레이커 체크
        val circuitCheck = riskGuardService.checkCircuitBreaker(request.questionId)
        if (!circuitCheck.passed) {
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.RISK_LIMIT_EXCEEDED,
                entityType = "ORDER",
                entityId = request.questionId,
                detail = circuitCheck.message
            )
            return CreateOrderResponse(
                success = false,
                message = circuitCheck.message!!
            )
        }

        // 5. MARKET 주문일 경우 상대 오더북의 최우선 가격으로 체결
        val orderPrice = if (orderType == OrderType.MARKET) {
            // 상대 오더북에서 최우선 가격 가져오기
            val oppositeSide = if (request.side == OrderSide.YES) OrderSide.NO else OrderSide.YES
            val oppositeOrders = orderRepository.findMatchableOrdersWithLock(
                questionId = request.questionId,
                side = oppositeSide,
                direction = OrderDirection.BUY,
                excludeMemberId = memberId,
                price = BigDecimal.ZERO  // 모든 가격 조회
            )

            if (oppositeOrders.isEmpty()) {
                // 호가가 없으면 체결 실패 (차감하지 않고 바로 반환)
                return CreateOrderResponse(
                    success = false,
                    message = "Market order failed: No executable quotes available."
                )
            }

            // 상대 호가의 최우선 가격 (= 내 주문의 체결 가격)
            val oppositePrice = oppositeOrders.first().price
            BigDecimal.ONE.subtract(oppositePrice)  // 역가격 계산
        } else {
            // LIMIT 주문: price는 이미 검증됨 (null이 아님)
            request.price!!
        }

        // === BUY/SELL 분기 처리 ===
        if (request.direction == OrderDirection.BUY) {
            // BUY 주문: USDC 예치
            val totalCost = BigDecimal(request.amount).multiply(orderPrice)
            if (member.usdcBalance < totalCost) {
                return CreateOrderResponse(
                    success = false,
                    message = "Insufficient balance. (Current: ${member.usdcBalance}, Required: $totalCost)"
                )
            }

            // 리스크 가드: 주문 금액 한도 체크
            val orderValueCheck = riskGuardService.checkOrderValueLimit(totalCost.toDouble())
            if (!orderValueCheck.passed) {
                auditService.log(
                    memberId = memberId,
                    action = com.predata.backend.domain.AuditAction.RISK_LIMIT_EXCEEDED,
                    entityType = "ORDER",
                    entityId = null,
                    detail = orderValueCheck.message
                )
                return CreateOrderResponse(
                    success = false,
                    message = orderValueCheck.message!!
                )
            }

            // USDC 차감 (예치)
            member.usdcBalance = member.usdcBalance.subtract(totalCost)
            memberRepository.save(member)

            transactionHistoryService.record(
                memberId = memberId,
                type = "BET",
                amount = totalCost.negate(),
                balanceAfter = member.usdcBalance,
                description = "주문 생성 (BUY) - Question #${request.questionId} ${request.side} (${orderType})",
                questionId = request.questionId
            )
        } else {
            // SELL 주문: reserved 기반으로 초과판매 방지 (quantity - reserved >= amount)
            try {
                positionService.reserveForSellOrder(
                    memberId = memberId,
                    questionId = request.questionId,
                    side = request.side,
                    reserveAmount = BigDecimal(request.amount)
                )
            } catch (e: IllegalStateException) {
                val msg = e.message ?: "Insufficient position."
                if (msg.startsWith("INSUFFICIENT_AVAILABLE_TO_SELL:")) {
                    val available = msg.substringAfter(":")
                    return CreateOrderResponse(
                        success = false,
                        message = "Exceeds available quantity for sale. (Currently available: ${available})"
                    )
                }
                return CreateOrderResponse(success = false, message = msg)
            }
        }

        // 6. 주문 생성
        val order = Order(
            memberId = memberId,
            questionId = request.questionId,
            orderType = orderType,
            side = request.side,
            direction = request.direction,
            price = orderPrice,
            amount = request.amount,
            remainingAmount = request.amount
        )

        val savedOrder = orderRepository.save(order)

        // Audit log: 주문 생성
        auditService.log(
            memberId = memberId,
            action = com.predata.backend.domain.AuditAction.ORDER_CREATE,
            entityType = "ORDER",
            entityId = savedOrder.id,
            detail = "${request.direction} ${request.side} ${request.amount} @ ${orderPrice} (${orderType})"
        )

        // 7. 매칭 실행
        val matchResult = matchOrder(savedOrder, question)

        // 8. MARKET 주문인 경우 미체결분 자동 취소 (IOC)
        if (orderType == OrderType.MARKET && savedOrder.remainingAmount > 0) {
            savedOrder.status = OrderStatus.CANCELLED
            savedOrder.remainingAmount = 0
            savedOrder.updatedAt = LocalDateTime.now()
            orderRepository.save(savedOrder)

            // BUY 주문만 환불 (SELL은 USDC 예치 없었으므로 환불 불필요)
            if (request.direction == OrderDirection.BUY) {
                // 미체결분 환불 = 미체결 수량 × 가격
                val unfilledQty = request.amount - matchResult.filledAmount
                val refundAmount = BigDecimal(unfilledQty).multiply(orderPrice)
                member.usdcBalance = member.usdcBalance.add(refundAmount)
                memberRepository.save(member)

                transactionHistoryService.record(
                    memberId = memberId,
                    type = "SETTLEMENT",
                    amount = refundAmount,
                    balanceAfter = member.usdcBalance,
                    description = "시장가 IOC 주문 미체결분 환불 (BUY) - Question #${request.questionId}",
                    questionId = request.questionId
                )
            }
            // SELL MARKET: 미체결분은 예약만 해제
            if (request.direction == OrderDirection.SELL) {
                val unfilledQty = request.amount - matchResult.filledAmount
                if (unfilledQty > 0) {
                    positionService.releaseSellReservation(
                        memberId = memberId,
                        questionId = request.questionId,
                        side = request.side,
                        releaseAmount = BigDecimal(unfilledQty)
                    )
                }
            }
        }

        return CreateOrderResponse(
            success = true,
            message = when {
                orderType == OrderType.MARKET && matchResult.filledAmount == 0L ->
                    "Market order was not filled. (Cancelled immediately)"
                orderType == OrderType.MARKET && matchResult.filledAmount < request.amount ->
                    "Market order partially filled. (${matchResult.filledAmount}/${request.amount}, unfilled portion auto-cancelled)"
                matchResult.filledAmount == request.amount -> "Order completely filled."
                matchResult.filledAmount > 0 -> "Partially filled. (${matchResult.filledAmount}/${request.amount})"
                else -> "Order added to order book."
            },
            orderId = savedOrder.id,
            filledAmount = matchResult.filledAmount,
            remainingAmount = savedOrder.remainingAmount
        )
    }

    /**
     * 주문 매칭 로직
     *
     * YES 매수 @ 0.60 → NO 매수 @ 0.40 이하와 매칭
     * (가격 합이 1.00이 되면 체결)
     */
    private fun matchOrder(order: Order, question: Question): MatchResult {
        // 반대 포지션의 역가격 계산
        val oppositePrice = BigDecimal.ONE.subtract(order.price)
        val oppositeSide = if (order.side == OrderSide.YES) OrderSide.NO else OrderSide.YES

        // 반대 방향 주문 조회 (락 적용)
        val matchableOrders = orderRepository.findMatchableOrdersWithLock(
            questionId = order.questionId,
            side = oppositeSide,
            direction = OrderDirection.BUY,
            excludeMemberId = order.memberId,
            price = oppositePrice
        )

        var totalFilled = 0L

        for (counterOrder in matchableOrders) {
            if (order.remainingAmount <= 0) break

            val fillAmount = minOf(order.remainingAmount, counterOrder.remainingAmount)
            val tradePrice = order.price  // Taker 가격 사용

            // 체결 기록 - Taker/Maker 모델
            // Taker = 주문을 넣어서 체결을 일으킨 쪽 (order)
            // Maker = 오더북에 대기 중이던 쪽 (counterOrder)
            val trade = Trade(
                questionId = order.questionId,
                takerOrderId = order.id!!,
                makerOrderId = counterOrder.id!!,
                price = tradePrice,
                amount = fillAmount,
                side = order.side
            )
            tradeRepository.save(trade)

            // 정산 집계를 위해 양쪽 모두 Activity 레코드 생성
            // Taker (주문자)
            activityRepository.save(Activity(
                memberId = order.memberId,
                questionId = order.questionId,
                activityType = ActivityType.BET,
                choice = if (order.side == OrderSide.YES) Choice.YES else Choice.NO,
                amount = fillAmount
            ))

            // Maker (상대방)
            activityRepository.save(Activity(
                memberId = counterOrder.memberId,
                questionId = counterOrder.questionId,
                activityType = ActivityType.BET,
                choice = if (counterOrder.side == OrderSide.YES) Choice.YES else Choice.NO,
                amount = fillAmount
            ))

            // 포지션 및 USDC 정산 (BUY/SELL 모델)
            // BUY: 포지션 증가 (USDC 이미 예치됨)
            // SELL: 포지션 감소, USDC 수령

            // Taker 처리
            if (order.direction == OrderDirection.BUY) {
                // BUY 주문: 포지션 증가 (netPosition으로 양쪽 포지션 불허 정책 적용)
                positionService.netPosition(
                    memberId = order.memberId,
                    questionId = order.questionId,
                    newSide = order.side,
                    newQty = BigDecimal(fillAmount),
                    newPrice = tradePrice
                )
            } else {
                // SELL 주문: 포지션 감소 (락 + 영속성 + 0 삭제)
                positionService.decreasePosition(
                    memberId = order.memberId,
                    questionId = order.questionId,
                    side = order.side,
                    decreaseAmount = BigDecimal(fillAmount)
                )

                // USDC 지급 (체결가 * 수량)
                val sellerProceeds = BigDecimal(fillAmount).multiply(tradePrice)
                val sellerMember = memberRepository.findByIdForUpdate(order.memberId)
                    ?: throw IllegalArgumentException("Member not found: ${order.memberId}")
                sellerMember.usdcBalance = sellerMember.usdcBalance.add(sellerProceeds)
                memberRepository.save(sellerMember)

                transactionHistoryService.record(
                    memberId = order.memberId,
                    type = "SELL_SETTLEMENT",
                    amount = sellerProceeds,
                    balanceAfter = sellerMember.usdcBalance,
                    description = "포지션 판매 체결 - Question #${order.questionId} ${order.side}",
                    questionId = order.questionId
                )
            }

            // Maker 처리
            if (counterOrder.direction == OrderDirection.BUY) {
                // BUY 주문: 포지션 증가
                positionService.netPosition(
                    memberId = counterOrder.memberId,
                    questionId = counterOrder.questionId,
                    newSide = counterOrder.side,
                    newQty = BigDecimal(fillAmount),
                    newPrice = oppositePrice
                )
            } else {
                // SELL 주문: 포지션 감소 (락 + 영속성 + 0 삭제)
                positionService.decreasePosition(
                    memberId = counterOrder.memberId,
                    questionId = counterOrder.questionId,
                    side = counterOrder.side,
                    decreaseAmount = BigDecimal(fillAmount)
                )

                // USDC 지급
                val sellerProceeds = BigDecimal(fillAmount).multiply(oppositePrice)
                val sellerMember = memberRepository.findByIdForUpdate(counterOrder.memberId)
                    ?: throw IllegalArgumentException("Member not found: ${counterOrder.memberId}")
                sellerMember.usdcBalance = sellerMember.usdcBalance.add(sellerProceeds)
                memberRepository.save(sellerMember)

                transactionHistoryService.record(
                    memberId = counterOrder.memberId,
                    type = "SELL_SETTLEMENT",
                    amount = sellerProceeds,
                    balanceAfter = sellerMember.usdcBalance,
                    description = "포지션 판매 체결 - Question #${counterOrder.questionId} ${counterOrder.side}",
                    questionId = counterOrder.questionId
                )
            }

            // 주문 수량 업데이트
            order.remainingAmount -= fillAmount
            counterOrder.remainingAmount -= fillAmount

            // 상태 업데이트
            updateOrderStatus(order)
            updateOrderStatus(counterOrder)

            orderRepository.save(counterOrder)

            totalFilled += fillAmount
        }

        orderRepository.save(order)

        // 풀 업데이트 (통계용)
        if (totalFilled > 0) {
            when (order.side) {
                OrderSide.YES -> question.yesBetPool += totalFilled
                OrderSide.NO -> question.noBetPool += totalFilled
            }
            question.totalBetPool += totalFilled
            questionRepository.save(question)

            // 가격 이력 기록 (체결 발생 시)
            recordPriceHistory(order.questionId, order.price)
        }

        return MatchResult(filledAmount = totalFilled)
    }

    /**
     * 가격 이력 기록
     */
    private fun recordPriceHistory(questionId: Long, lastTradePrice: BigDecimal) {
        try {
            val orderBook = orderBookService.getOrderBook(questionId)
            val bestBid = orderBook.bids.firstOrNull()?.price
            val bestAsk = orderBook.asks.firstOrNull()?.price
            val midPrice = if (bestBid != null && bestAsk != null) {
                bestBid.add(bestAsk).divide(BigDecimal("2.00"), 2, java.math.RoundingMode.HALF_UP)
            } else null
            val spread = orderBook.spread

            val priceHistory = com.predata.backend.domain.PriceHistory(
                questionId = questionId,
                midPrice = midPrice,
                lastTradePrice = lastTradePrice,
                spread = spread
            )
            priceHistoryRepository.save(priceHistory)
        } catch (e: Exception) {
            // Price history recording failure is not critical, just log
            logger.warn("[PriceHistory] Price history recording failed: {}", e.message)
        }
    }

    private fun updateOrderStatus(order: Order) {
        order.status = when {
            order.remainingAmount == 0L -> OrderStatus.FILLED
            order.remainingAmount < order.amount -> OrderStatus.PARTIAL
            else -> OrderStatus.OPEN
        }
        order.updatedAt = LocalDateTime.now()
    }

    /**
     * 주문 취소
     */
    @Transactional
    fun cancelOrder(orderId: Long, memberId: Long): CancelOrderResponse {
        val order = orderRepository.findByIdWithLock(orderId)
            ?: return CancelOrderResponse(
                success = false,
                message = "Order not found."
            )

        if (order.memberId != memberId) {
            return CancelOrderResponse(
                success = false,
                message = "You can only cancel your own orders."
            )
        }

        if (order.status !in listOf(OrderStatus.OPEN, OrderStatus.PARTIAL)) {
            return CancelOrderResponse(
                success = false,
                message = "Order cannot be cancelled. (Status: ${order.status})"
            )
        }

        // BUY/SELL에 따른 취소 처리
        val refundAmount = if (order.direction == OrderDirection.BUY) {
            // BUY 주문: USDC 환불 = 미체결 수량 × 가격
            val amount = BigDecimal(order.remainingAmount).multiply(order.price)
            val member = memberRepository.findByIdForUpdate(memberId)
            if (member != null) {
                member.usdcBalance = member.usdcBalance.add(amount)
                memberRepository.save(member)

                transactionHistoryService.record(
                    memberId = memberId,
                    type = "SETTLEMENT",
                    amount = amount,
                    balanceAfter = member.usdcBalance,
                    description = "주문 취소 환불 (BUY) - Order #$orderId",
                    questionId = order.questionId
                )
            }
            amount
        } else {
            // SELL 주문: 미체결 수량만큼 예약 해제
            if (order.remainingAmount > 0) {
                positionService.releaseSellReservation(
                    memberId = memberId,
                    questionId = order.questionId,
                    side = order.side,
                    releaseAmount = BigDecimal(order.remainingAmount)
                )
            }
            BigDecimal.ZERO
        }

        order.status = OrderStatus.CANCELLED
        order.remainingAmount = 0
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)

        // Audit log: 주문 취소
        auditService.log(
            memberId = memberId,
            action = com.predata.backend.domain.AuditAction.ORDER_CANCEL,
            entityType = "ORDER",
            entityId = orderId,
            detail = "Order cancelled, refund: $refundAmount"
        )

        return CancelOrderResponse(
            success = true,
            message = "Order cancelled successfully.",
            refundedAmount = refundAmount.setScale(0, java.math.RoundingMode.DOWN).toLong()
        )
    }

    /**
     * 회원의 활성 주문 조회
     */
    fun getActiveOrders(memberId: Long): List<OrderResponse> {
        val activeStatuses = listOf(OrderStatus.OPEN, OrderStatus.PARTIAL)
        return orderRepository.findByMemberIdAndStatusIn(memberId, activeStatuses)
            .map { it.toResponse() }
    }

    /**
     * 회원의 특정 질문에 대한 주문 조회
     */
    fun getOrdersByQuestion(memberId: Long, questionId: Long): List<OrderResponse> {
        return orderRepository.findByMemberIdAndQuestionId(memberId, questionId)
            .map { it.toResponse() }
    }

    private fun Order.toResponse() = OrderResponse(
        orderId = this.id!!,
        memberId = this.memberId,
        questionId = this.questionId,
        side = this.side,
        direction = this.direction,
        price = this.price,
        amount = this.amount,
        remainingAmount = this.remainingAmount,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    data class MatchResult(val filledAmount: Long)
}
