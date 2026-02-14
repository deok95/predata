package com.predata.backend.controller

import com.predata.backend.service.TransactionHistoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class TransactionController(
    private val transactionHistoryService: TransactionHistoryService
) {

    /**
     * GET /api/transactions/my?memberId=1&type=BET&page=0&size=20
     * Returns paginated transaction history for the authenticated user.
     */
    @GetMapping("/my")
    fun getMyTransactions(
        @RequestParam memberId: Long,
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        return try {
            val result = transactionHistoryService.getHistory(memberId, type, page, size)
            ResponseEntity.ok(mapOf(
                "content" to result.content.map { tx ->
                    mapOf(
                        "id" to tx.id,
                        "type" to tx.type,
                        "amount" to tx.amount.toDouble(),
                        "balanceAfter" to tx.balanceAfter.toDouble(),
                        "description" to tx.description,
                        "questionId" to tx.questionId,
                        "txHash" to tx.txHash,
                        "createdAt" to tx.createdAt.toString()
                    )
                },
                "totalElements" to result.totalElements,
                "totalPages" to result.totalPages,
                "page" to result.number,
                "size" to result.size
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message))
        }
    }
}
