package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.OrderBookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class PriceController(
    private val orderBookService: OrderBookService
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
}
