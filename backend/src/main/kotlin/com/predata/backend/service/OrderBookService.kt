package com.predata.backend.service

import com.predata.backend.dto.OrderBookLevel
import com.predata.backend.dto.OrderBookResponse
import com.predata.backend.repository.OrderRepository
import com.predata.backend.repository.TradeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class OrderBookService(
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository,
    private val priceHistoryRepository: com.predata.backend.repository.PriceHistoryRepository,
    private val questionRepository: com.predata.backend.repository.QuestionRepository
) {

    /**
     * 오더북 조회
     * - bids: YES 매수 주문 (높은 가격순)
     * - asks: NO 매수 주문 = YES 매도 (낮은 가격순, 역가격 표시)
     */
    fun getOrderBook(questionId: Long): OrderBookResponse {
        // VOTING 상태에서는 빈 오더북 반환
        val question = questionRepository.findById(questionId).orElse(null)
        if (question != null && question.status == com.predata.backend.domain.QuestionStatus.VOTING) {
            return OrderBookResponse(
                questionId = questionId,
                bids = emptyList(),
                asks = emptyList(),
                lastPrice = null,
                spread = null
            )
        }

        val yesBids = orderRepository.findYesBids(questionId)
        val noAsks = orderRepository.findNoAsks(questionId)

        // 가격별 집계 - YES Bids
        val bidLevels = yesBids
            .groupBy { it.price }
            .map { (price, orders) ->
                OrderBookLevel(
                    price = price,
                    amount = orders.sumOf { it.remainingAmount },
                    count = orders.size
                )
            }
            .sortedByDescending { it.price }

        // NO 매수는 YES 매도로 변환 (1 - price)
        val askLevels = noAsks
            .groupBy { BigDecimal.ONE.subtract(it.price) }
            .map { (yesPrice, orders) ->
                OrderBookLevel(
                    price = yesPrice,
                    amount = orders.sumOf { it.remainingAmount },
                    count = orders.size
                )
            }
            .sortedBy { it.price }

        // 스프레드 계산
        val bestBid = bidLevels.firstOrNull()?.price
        val bestAsk = askLevels.firstOrNull()?.price
        val spread = if (bestBid != null && bestAsk != null) {
            bestAsk.subtract(bestBid)
        } else null

        // 최근 체결가
        val lastTrade = tradeRepository.findTopByQuestionIdOrderByExecutedAtDesc(questionId)

        return OrderBookResponse(
            questionId = questionId,
            bids = bidLevels,
            asks = askLevels,
            lastPrice = lastTrade?.price,
            spread = spread
        )
    }

    /**
     * Mid-price 계산 (최우선 bid + 최우선 ask) / 2
     */
    fun getMidPrice(questionId: Long): BigDecimal? {
        val orderBook = getOrderBook(questionId)
        val bestBid = orderBook.bids.firstOrNull()?.price
        val bestAsk = orderBook.asks.firstOrNull()?.price

        return if (bestBid != null && bestAsk != null) {
            bestBid.add(bestAsk).divide(BigDecimal("2.00"), 2, java.math.RoundingMode.HALF_UP)
        } else {
            null
        }
    }

    /**
     * 가격 정보 조회 (mid-price, bestBid, bestAsk, lastTradePrice, spread)
     */
    fun getPriceInfo(questionId: Long): PriceInfo {
        val orderBook = getOrderBook(questionId)
        val bestBid = orderBook.bids.firstOrNull()?.price
        val bestAsk = orderBook.asks.firstOrNull()?.price
        val midPrice = if (bestBid != null && bestAsk != null) {
            bestBid.add(bestAsk).divide(BigDecimal("2.00"), 2, java.math.RoundingMode.HALF_UP)
        } else null

        // 최근 가격 이력 조회 (없으면 현재 가격 사용)
        val recentHistory = priceHistoryRepository.findTopByQuestionIdOrderByTimestampDesc(questionId)

        return PriceInfo(
            questionId = questionId,
            midPrice = midPrice,
            bestBid = bestBid,
            bestAsk = bestAsk,
            lastTradePrice = recentHistory?.lastTradePrice ?: orderBook.lastPrice,
            spread = orderBook.spread
        )
    }

    data class PriceInfo(
        val questionId: Long,
        val midPrice: BigDecimal?,
        val bestBid: BigDecimal?,
        val bestAsk: BigDecimal?,
        val lastTradePrice: BigDecimal?,
        val spread: BigDecimal?
    )
}
