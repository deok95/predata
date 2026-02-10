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
    private val tradeRepository: TradeRepository
) {

    /**
     * 오더북 조회
     * - bids: YES 매수 주문 (높은 가격순)
     * - asks: NO 매수 주문 = YES 매도 (낮은 가격순, 역가격 표시)
     */
    fun getOrderBook(questionId: Long): OrderBookResponse {
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
}
