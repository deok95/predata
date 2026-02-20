package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.TransactionHistoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transactions")
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
    ): ResponseEntity<ApiEnvelope<TransactionHistoryResponse>> {
        val result = transactionHistoryService.getHistory(memberId, type, page, size)

        val items = result.content.map { tx ->
            TransactionItemDto(
                id = tx.id,
                type = tx.type,
                amount = tx.amount.toDouble(),
                balanceAfter = tx.balanceAfter.toDouble(),
                description = tx.description,
                questionId = tx.questionId,
                txHash = tx.txHash,
                createdAt = tx.createdAt.toString()
            )
        }

        return ResponseEntity.ok(
            ApiEnvelope.ok(
                TransactionHistoryResponse(
                    content = items,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                    page = result.number,
                    size = result.size
                )
            )
        )
    }
}

data class TransactionItemDto(
    val id: Long?,
    val type: String,
    val amount: Double,
    val balanceAfter: Double,
    val description: String?,
    val questionId: Long?,
    val txHash: String?,
    val createdAt: String
)

data class TransactionHistoryResponse(
    val content: List<TransactionItemDto>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)
