package com.predata.backend.service

import com.predata.backend.domain.*
import com.predata.backend.dto.CreateOrderRequest
import com.predata.backend.repository.OrderRepository
import com.predata.backend.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 마켓메이커 서비스
 * - BREAK -> BETTING 전환 시 초기 유동성 제공
 * - YES/NO 양쪽에 대해 bid/ask 3레벨씩 시딩
 */
@Service
class MarketMakerService(
    private val orderRepository: OrderRepository,
    private val questionRepository: QuestionRepository,
    private val positionService: PositionService,
    private val orderMatchingService: OrderMatchingService,
    @Value("\${app.market-maker.member-id}") private val marketMakerMemberId: Long
) {
    private val logger = LoggerFactory.getLogger(MarketMakerService::class.java)

    companion object {
        // 초기 오더북 스펙 (고정)
        val BID_LEVELS = listOf(
            BigDecimal("0.48"),
            BigDecimal("0.46"),
            BigDecimal("0.44")
        )
        val ASK_LEVELS = listOf(
            BigDecimal("0.52"),
            BigDecimal("0.54"),
            BigDecimal("0.56")
        )
        const val SHARES_PER_LEVEL = 200L

        // 초기 포지션 부여량 (SELL 주문용)
        // 각 side마다 3레벨 * 200 = 600 shares + 여유분 = 1000 shares
        val INITIAL_POSITION_PER_SIDE = BigDecimal("1000")
    }

    /**
     * 오더북 시딩 (중복 방지 포함)
     * - YES/NO 양쪽에 대해 bid/ask 생성
     * - 이미 시딩된 경우 스킵
     */
    @Transactional
    fun seedOrderBookIfNeeded(questionId: Long) {
        try {
            // 1. 질문 상태 확인 (row lock)
            val question = questionRepository.findByIdWithLock(questionId)
                ?: run {
                    logger.warn("[MarketMaker] Question #$questionId not found")
                    return
                }

            if (question.status != QuestionStatus.BETTING) {
                logger.info("[MarketMaker] Question #$questionId is not in BETTING status (current: ${question.status})")
                return
            }

            // 2. 중복 시딩 방지 체크
            val alreadySeeded = orderRepository.existsByQuestionIdAndMemberIdAndStatusIn(
                questionId = questionId,
                memberId = marketMakerMemberId,
                statuses = listOf(OrderStatus.OPEN, OrderStatus.PARTIAL)
            )

            if (alreadySeeded) {
                logger.info("[MarketMaker] Question #$questionId already seeded, skipping")
                return
            }

            logger.info("[MarketMaker] Starting orderbook seeding for question #$questionId")

            // 3. 초기 포지션 부여 (YES/NO 각각)
            positionService.grantInitialPosition(
                memberId = marketMakerMemberId,
                questionId = questionId,
                side = OrderSide.YES,
                qty = INITIAL_POSITION_PER_SIDE
            )
            positionService.grantInitialPosition(
                memberId = marketMakerMemberId,
                questionId = questionId,
                side = OrderSide.NO,
                qty = INITIAL_POSITION_PER_SIDE
            )

            logger.info("[MarketMaker] Initial positions granted: YES=$INITIAL_POSITION_PER_SIDE, NO=$INITIAL_POSITION_PER_SIDE")

            // 4. 오더북 시딩: YES 및 NO 각각
            seedSide(questionId, OrderSide.YES)
            seedSide(questionId, OrderSide.NO)

            logger.info("[MarketMaker] Orderbook seeding completed for question #$questionId")

        } catch (e: Exception) {
            logger.error("[MarketMaker] Failed to seed orderbook for question #$questionId: ${e.message}", e)
            // 시딩 실패해도 질문 전환은 성공해야 하므로 예외를 던지지 않음
        }
    }

    /**
     * 특정 side (YES 또는 NO)에 대한 bid/ask 주문 생성
     */
    private fun seedSide(questionId: Long, side: OrderSide) {
        // BID 주문 (BUY): 낮은 가격에 매수 대기
        BID_LEVELS.forEach { price ->
            createMarketMakerOrder(
                questionId = questionId,
                side = side,
                direction = OrderDirection.BUY,
                price = price,
                amount = SHARES_PER_LEVEL
            )
        }

        // ASK 주문 (SELL): 높은 가격에 매도 대기
        ASK_LEVELS.forEach { price ->
            createMarketMakerOrder(
                questionId = questionId,
                side = side,
                direction = OrderDirection.SELL,
                price = price,
                amount = SHARES_PER_LEVEL
            )
        }

        logger.info("[MarketMaker] Seeded $side: ${BID_LEVELS.size} bids + ${ASK_LEVELS.size} asks")
    }

    /**
     * 마켓메이커 주문 생성 (OrderMatchingService 사용)
     */
    private fun createMarketMakerOrder(
        questionId: Long,
        side: OrderSide,
        direction: OrderDirection,
        price: BigDecimal,
        amount: Long
    ) {
        try {
            val request = CreateOrderRequest(
                questionId = questionId,
                orderType = OrderType.LIMIT,
                side = side,
                direction = direction,
                price = price,
                amount = amount
            )

            val response = orderMatchingService.createOrder(marketMakerMemberId, request)

            if (!response.success) {
                logger.error("[MarketMaker] Order creation failed: ${response.message}")
            } else {
                logger.debug("[MarketMaker] Order created: $direction $side @ $price x $amount")
            }
        } catch (e: Exception) {
            logger.error("[MarketMaker] Failed to create order: $direction $side @ $price", e)
        }
    }
}
