package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.TransactionHistoryService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "finance-wallet", description = "Transaction APIs")
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionHistoryService: TransactionHistoryService
) {

    /**
     * GET /api/transactions/my?type=BET&page=0&size=20
     * Returns paginated transaction history for the authenticated user.
     * memberId는 JWT claim에서 추출한다 (쿼리 파라미터로 받지 않음).
     */
    @GetMapping("/my")
    fun getMyTransactions(
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<TransactionHistoryResponse>> {
        val memberId = httpRequest.authenticatedMemberId()
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
