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
        // 1. 베팅 일시 중지 체크 (쿨다운)
        val suspensionStatus = bettingSuspensionService.isBettingSuspendedByQuestionId(request.questionId)
        if (suspensionStatus.suspended) {
            return CreateOrderResponse(
                success = false,
                message = "⚠️ 골 직후 베팅이 일시 중지되었습니다. ${suspensionStatus.remainingSeconds}초 후 재개됩니다."
            )
        }

        // 2. 회원 및 잔액 확인
        val member = memberRepository.findById(memberId)
            .orElse(null) ?: return CreateOrderResponse(
                success = false,
                message = "회원을 찾을 수 없습니다."
            )

        if (member.isBanned) {
            return CreateOrderResponse(
                success = false,
                message = "계정이 정지되었습니다."
            )
        }

        // 3. 주문 타입 결정
        val orderType = request.orderType ?: OrderType.LIMIT  // 기본값: LIMIT

        // 2. 질문 상태 확인
        val question = questionRepository.findByIdWithLock(request.questionId)
            ?: return CreateOrderResponse(
                success = false,
                message = "질문을 찾을 수 없습니다."
            )

        if (question.status != QuestionStatus.BETTING) {
            return CreateOrderResponse(
                success = false,
                message = "베팅 기간이 아닙니다. (현재: ${question.status})"
            )
        }

        if (question.expiredAt.isBefore(LocalDateTime.now())) {
            return CreateOrderResponse(
                success = false,
                message = "베팅 기간이 만료되었습니다."
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

        // 4. MARKET 주문일 경우 상대 오더북의 최우선 가격으로 체결
        val orderPrice = if (orderType == OrderType.MARKET) {
            // 상대 오더북에서 최우선 가격 가져오기
            val oppositeSide = if (request.side == OrderSide.YES) OrderSide.NO else OrderSide.YES
            val oppositeOrders = orderRepository.findMatchableOrdersWithLock(
                questionId = request.questionId,
                side = oppositeSide,
                price = BigDecimal.ZERO  // 모든 가격 조회
            )

            if (oppositeOrders.isEmpty()) {
                // 호가가 없으면 체결 실패 (차감하지 않고 바로 반환)
                return CreateOrderResponse(
                    success = false,
                    message = "시장가 주문 실패: 체결 가능한 호가가 없습니다."
                )
            }

            // 상대 호가의 최우선 가격 (= 내 주문의 체결 가격)
            val oppositePrice = oppositeOrders.first().price
            BigDecimal.ONE.subtract(oppositePrice)  // 역가격 계산
        } else {
            request.price
        }

        // === BUY/SELL 분기 처리 ===
        if (request.direction == OrderDirection.BUY) {
            // BUY 주문: USDC 예치
            val totalCost = BigDecimal(request.amount).multiply(orderPrice)
            if (member.usdcBalance < totalCost) {
                return CreateOrderResponse(
                    success = false,
                    message = "잔액이 부족합니다. (보유: ${member.usdcBalance}, 필요: $totalCost)"
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
            // SELL 주문: 포지션 체크 및 락
            val position = positionService.getPositionsByQuestion(memberId, request.questionId)
                .find { it.side == request.side }

            if (position == null || position.quantity < BigDecimal(request.amount)) {
                return CreateOrderResponse(
                    success = false,
                    message = "보유 포지션이 부족합니다. (보유: ${position?.quantity ?: 0}, 필요: ${request.amount})"
                )
            }

            // SELL 주문은 USDC 차감 없음 (포지션을 담보로 사용)
            // TODO: 포지션 락 로직 (중복 판매 방지) - 향후 구현
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
                description = "시장가 IOC 주문 미체결분 환불 - Question #${request.questionId}",
                questionId = request.questionId
            )
        }

        return CreateOrderResponse(
            success = true,
            message = when {
                orderType == OrderType.MARKET && matchResult.filledAmount == 0L ->
                    "시장가 주문이 체결되지 않았습니다. (즉시 취소됨)"
                orderType == OrderType.MARKET && matchResult.filledAmount < request.amount ->
                    "시장가 주문이 부분 체결되었습니다. (${matchResult.filledAmount}/${request.amount}, 미체결분 자동 취소)"
                matchResult.filledAmount == request.amount -> "주문이 완전 체결되었습니다."
                matchResult.filledAmount > 0 -> "부분 체결되었습니다. (${matchResult.filledAmount}/${request.amount})"
                else -> "주문이 오더북에 등록되었습니다."
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
            price = oppositePrice
        )

        var totalFilled = 0L

        for (counterOrder in matchableOrders) {
            if (order.remainingAmount <= 0) break

            val fillAmount = minOf(order.remainingAmount, counterOrder.remainingAmount)
            val tradePrice = order.price  // Taker 가격 사용

            // 체결 기록
            val trade = Trade(
                questionId = order.questionId,
                buyOrderId = order.id!!,
                sellOrderId = counterOrder.id!!,
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
                // SELL 주문: 포지션 감소, USDC 지급
                val position = positionService.getPositionsByQuestion(order.memberId, order.questionId)
                    .find { it.side == order.side }
                if (position != null) {
                    position.quantity = position.quantity.subtract(BigDecimal(fillAmount))
                    // Position will be saved by service layer
                }

                // USDC 지급 (체결가 * 수량)
                val sellerProceeds = BigDecimal(fillAmount).multiply(tradePrice)
                val sellerMember = memberRepository.findById(order.memberId).orElseThrow()
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
                // SELL 주문: 포지션 감소, USDC 지급
                val position = positionService.getPositionsByQuestion(counterOrder.memberId, counterOrder.questionId)
                    .find { it.side == counterOrder.side }
                if (position != null) {
                    position.quantity = position.quantity.subtract(BigDecimal(fillAmount))
                }

                // USDC 지급
                val sellerProceeds = BigDecimal(fillAmount).multiply(oppositePrice)
                val sellerMember = memberRepository.findById(counterOrder.memberId).orElseThrow()
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
            // 가격 이력 기록 실패는 치명적이지 않으므로 로그만 남김
            logger.warn("[PriceHistory] 가격 이력 기록 실패: {}", e.message)
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
                message = "주문을 찾을 수 없습니다."
            )

        if (order.memberId != memberId) {
            return CancelOrderResponse(
                success = false,
                message = "본인의 주문만 취소할 수 있습니다."
            )
        }

        if (order.status !in listOf(OrderStatus.OPEN, OrderStatus.PARTIAL)) {
            return CancelOrderResponse(
                success = false,
                message = "취소할 수 없는 주문입니다. (상태: ${order.status})"
            )
        }

        // BUY/SELL에 따른 취소 처리
        val refundAmount = if (order.direction == OrderDirection.BUY) {
            // BUY 주문: USDC 환불 = 미체결 수량 × 가격
            val amount = BigDecimal(order.remainingAmount).multiply(order.price)
            val member = memberRepository.findById(memberId).orElse(null)
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
            // SELL 주문: 포지션 락 해제 (TODO: 향후 구현)
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
            message = "주문이 취소되었습니다.",
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
