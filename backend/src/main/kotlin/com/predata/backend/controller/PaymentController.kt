package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.PaymentVerificationService
import com.predata.backend.service.WithdrawalService
import com.predata.backend.service.WithdrawResponse
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentVerificationService: PaymentVerificationService,
    private val withdrawalService: WithdrawalService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * $ 잔액 충전 — USDC 트랜잭션 검증 후 잔액 반영
     * POST /api/payments/verify-deposit
     */
    @PostMapping("/verify-deposit")
    fun verifyDeposit(
        @RequestBody request: VerifyDepositRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Any> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        log.info("충전 검증 요청: memberId=$authenticatedMemberId, txHash=${request.txHash}, amount=${request.amount}, fromAddress=${request.fromAddress}")
        val result = paymentVerificationService.verifyDeposit(
            memberId = authenticatedMemberId,
            txHash = request.txHash,
            amount = BigDecimal(request.amount.toString()),
            fromAddress = request.fromAddress
        )
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 출금 — $ 잔액 차감 후 USDC 전송
     * POST /api/payments/withdraw
     */
    @PostMapping("/withdraw")
    fun withdraw(
        @RequestBody request: WithdrawRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<WithdrawResponse>> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        log.info("출금 요청: memberId=$authenticatedMemberId, amount=${request.amount}, wallet=${request.walletAddress}")
        val result = withdrawalService.withdraw(
            memberId = authenticatedMemberId,
            amount = BigDecimal(request.amount.toString()),
            walletAddress = request.walletAddress
        )
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }
}

// ===== Request DTOs =====
// memberId는 JWT에서 추출하므로 DTO에서 제거
data class VerifyDepositRequest(
    val txHash: String,
    val amount: Double,
    val fromAddress: String? = null
)

data class WithdrawRequest(
    val amount: Double,
    val walletAddress: String
)
