package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.OrderBookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class PriceController(
    private val orderBookService: OrderBookService,
    private val priceHistoryRepository: com.predata.backend.repository.PriceHistoryRepository
) {

    /**
     * 가격 정보 조회 (mid-price, bestBid, bestAsk, lastTradePrice, spread)
     * GET /api/questions/{id}/price
     */
    @GetMapping("/questions/{id}/price")
    fun getPrice(@PathVariable id: Long): ResponseEntity<ApiEnvelope<OrderBookService.PriceInfo>> {
        val priceInfo = orderBookService.getPriceInfo(id)
        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = priceInfo
            )
        )
    }

    /**
     * 가격 이력 조회
     * GET /api/questions/{id}/price-history?interval=1m&limit=100
     */
    @GetMapping("/questions/{id}/price-history")
    fun getPriceHistory(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "1m") interval: String,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<ApiEnvelope<List<com.predata.backend.dto.PriceHistoryResponse>>> {
        // PriceHistoryRepository의 findRecentByQuestionId 메서드 사용
        val history = priceHistoryRepository.findRecentByQuestionId(id, limit)
            .map { priceHistory ->
                com.predata.backend.dto.PriceHistoryResponse(
                    timestamp = priceHistory.timestamp,
                    midPrice = priceHistory.midPrice,
                    lastTradePrice = priceHistory.lastTradePrice,
                    spread = priceHistory.spread
                )
            }

        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = history
            )
        )
    }
}
