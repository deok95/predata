package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.service.PaymentVerificationService
import com.predata.backend.service.WithdrawalService
import com.predata.backend.service.WithdrawResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
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
        return try {
            val authenticatedMemberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
                ?: throw IllegalArgumentException("인증 정보를 찾을 수 없습니다.")

            log.info("충전 검증 요청: memberId=$authenticatedMemberId, txHash=${request.txHash}, amount=${request.amount}, fromAddress=${request.fromAddress}")
            val result = paymentVerificationService.verifyDeposit(
                memberId = authenticatedMemberId,
                txHash = request.txHash,
                amount = BigDecimal(request.amount.toString()),
                fromAddress = request.fromAddress
            )
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            log.warn("충전 검증 실패: ${e.message}")
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message))
        } catch (e: Exception) {
            log.error("충전 검증 오류", e)
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to "서버 오류가 발생했습니다."))
        }
    }

    /**
     * 출금 — $ 잔액 차감 후 USDC 전송
     * POST /api/payments/withdraw
     */
    @PostMapping("/withdraw")
    fun withdraw(
        @RequestBody request: WithdrawRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Any> {
        return try {
            val authenticatedMemberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
                ?: throw IllegalArgumentException("인증 정보를 찾을 수 없습니다.")

            log.info("출금 요청: memberId=$authenticatedMemberId, amount=${request.amount}, wallet=${request.walletAddress}")
            val result = withdrawalService.withdraw(
                memberId = authenticatedMemberId,
                amount = BigDecimal(request.amount.toString()),
                walletAddress = request.walletAddress
            )
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            log.warn("출금 실패: ${e.message}")
            ResponseEntity.badRequest().body(
                WithdrawResponse(
                    success = false,
                    message = e.message ?: "출금에 실패했습니다."
                )
            )
        } catch (e: Exception) {
            log.error("출금 오류", e)
            ResponseEntity.internalServerError().body(
                WithdrawResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다."
                )
            )
        }
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
