package com.predata.backend.controller

import com.predata.backend.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = ["http://localhost:3000"])
class TicketPurchaseController(
    private val ticketPurchaseService: TicketPurchaseService
) {

    /**
     * 티켓 구매
     * POST /api/tickets/purchase
     */
    @PostMapping("/purchase")
    fun purchaseTickets(@RequestBody request: TicketPurchaseRequest): ResponseEntity<TicketPurchaseResponse> {
        return try {
            val response = ticketPurchaseService.purchaseTickets(request.memberId, request.quantity)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                TicketPurchaseResponse(
                    success = false,
                    purchasedQuantity = 0,
                    totalCost = 0,
                    remainingTickets = 0,
                    remainingPoints = 0,
                    message = e.message ?: "티켓 구매에 실패했습니다."
                )
            )
        }
    }

    /**
     * 티켓 구매 내역
     * GET /api/tickets/history/{memberId}
     */
    @GetMapping("/history/{memberId}")
    fun getPurchaseHistory(@PathVariable memberId: Long): ResponseEntity<List<TicketPurchaseHistory>> {
        val history = ticketPurchaseService.getPurchaseHistory(memberId)
        return ResponseEntity.ok(history)
    }
}

data class TicketPurchaseRequest(
    val memberId: Long,
    val quantity: Int
)
