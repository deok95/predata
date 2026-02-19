package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.service.OrderBookService
import com.predata.backend.service.OrderMatchingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class OrderController(
    private val orderMatchingService: OrderMatchingService,
    private val orderBookService: OrderBookService,
    private val bettingSuspensionService: com.predata.backend.service.BettingSuspensionService
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
        @Valid @RequestBody request: CreateOrderRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<CreateOrderResponse> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                CreateOrderResponse(success = false, message = "Authentication required.")
            )

        // 베팅 일시 중지 체크 (쿨다운)
        val suspensionStatus = bettingSuspensionService.isBettingSuspendedByQuestionId(request.questionId)
        if (suspensionStatus.suspended) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                CreateOrderResponse(
                    success = false,
                    message = "⚠️ Betting is temporarily suspended after a goal. It will resume in ${suspensionStatus.remainingSeconds} seconds."
                )
            )
        }

        val response = orderMatchingService.createOrder(authenticatedMemberId, request)
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
        httpRequest: HttpServletRequest
    ): ResponseEntity<CancelOrderResponse> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                CancelOrderResponse(success = false, message = "Authentication required.")
            )

        val response = orderMatchingService.cancelOrder(id, authenticatedMemberId)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    /**
     * GET /api/orders/me
     * 본인의 활성 주문 조회 (JWT 인증 사용)
     */
    @GetMapping("/orders/me")
    fun getMyActiveOrders(httpRequest: HttpServletRequest): ResponseEntity<List<OrderResponse>> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        val orders = orderMatchingService.getActiveOrders(authenticatedMemberId)
        return ResponseEntity.ok(orders)
    }

    /**
     * GET /api/orders/me/question/{questionId}
     * 본인의 특정 질문에 대한 주문 조회 (JWT 인증 사용)
     */
    @GetMapping("/orders/me/question/{questionId}")
    fun getMyOrdersByQuestion(
        @PathVariable questionId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<List<OrderResponse>> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        val orders = orderMatchingService.getOrdersByQuestion(authenticatedMemberId, questionId)
        return ResponseEntity.ok(orders)
    }
}
