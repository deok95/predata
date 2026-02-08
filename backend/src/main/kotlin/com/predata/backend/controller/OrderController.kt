package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.service.OrderBookService
import com.predata.backend.service.OrderMatchingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class OrderController(
    private val orderMatchingService: OrderMatchingService,
    private val orderBookService: OrderBookService
) {

    /**
     * GET /api/questions/{id}/orderbook
     * 오더북 조회
     */
    @GetMapping("/questions/{id}/orderbook")
    fun getOrderBook(@PathVariable id: Long): ResponseEntity<OrderBookResponse> {
        val orderBook = orderBookService.getOrderBook(id)
        return ResponseEntity.ok(orderBook)
    }

    /**
     * POST /api/orders
     * Limit Order 생성
     */
    @PostMapping("/orders")
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<CreateOrderResponse> {
        val response = orderMatchingService.createOrder(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    /**
     * DELETE /api/orders/{id}
     * 주문 취소
     */
    @DeleteMapping("/orders/{id}")
    fun cancelOrder(
        @PathVariable id: Long,
        @RequestParam memberId: Long
    ): ResponseEntity<CancelOrderResponse> {
        val response = orderMatchingService.cancelOrder(id, memberId)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    /**
     * GET /api/orders/member/{memberId}
     * 회원의 활성 주문 조회
     */
    @GetMapping("/orders/member/{memberId}")
    fun getActiveOrders(@PathVariable memberId: Long): ResponseEntity<List<OrderResponse>> {
        val orders = orderMatchingService.getActiveOrders(memberId)
        return ResponseEntity.ok(orders)
    }

    /**
     * GET /api/orders/member/{memberId}/question/{questionId}
     * 회원의 특정 질문에 대한 주문 조회
     */
    @GetMapping("/orders/member/{memberId}/question/{questionId}")
    fun getOrdersByQuestion(
        @PathVariable memberId: Long,
        @PathVariable questionId: Long
    ): ResponseEntity<List<OrderResponse>> {
        val orders = orderMatchingService.getOrdersByQuestion(memberId, questionId)
        return ResponseEntity.ok(orders)
    }
}
