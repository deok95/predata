package com.predata.backend.controller.amm

import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.dto.amm.*
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.ForbiddenException
import com.predata.backend.exception.NotFoundException
import com.predata.backend.service.amm.SwapService
import com.predata.backend.util.authenticatedMemberId
import com.predata.backend.util.authenticatedRole
import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api")
class SwapController(
    private val retryableSwapFacade: com.predata.backend.service.amm.RetryableSwapFacade,
    private val swapService: SwapService
) {

    /**
     * 스왑 실행 (BUY 또는 SELL)
     * 인증 필수 (JWT)
     */
    @PostMapping("/swap")
    fun executeSwap(
        @RequestBody request: SwapRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<SwapResponse>> {
        val memberId = httpRequest.authenticatedMemberId()

        return try {
            val response = retryableSwapFacade.executeSwapWithRetry(memberId, request)
            ResponseEntity.ok(ApiEnvelope.ok(response))
        } catch (e: OptimisticLockingFailureException) {
            throw ConflictException("Trading is congested. Please try again later.")
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw ConflictException("Trading is congested. Please try again later.")
        }
    }

    /**
     * 스왑 시뮬레이션
     * 인증 불필요
     */
    @GetMapping("/swap/simulate")
    fun simulateSwap(
        @RequestParam questionId: Long,
        @RequestParam action: SwapAction,
        @RequestParam outcome: ShareOutcome,
        @RequestParam amount: BigDecimal
    ): ResponseEntity<ApiEnvelope<SwapSimulationResponse>> {
        val response = swapService.simulateSwap(questionId, action, outcome, amount)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 풀 상태 조회
     * 인증 불필요
     */
    @GetMapping("/pool/{questionId}")
    fun getPoolState(
        @PathVariable questionId: Long
    ): ResponseEntity<ApiEnvelope<PoolStateResponse>> {
        return try {
            val response = swapService.getPoolState(questionId)
            ResponseEntity.ok(ApiEnvelope.ok(response))
        } catch (e: IllegalArgumentException) {
            throw NotFoundException(e.message ?: "Market pool not found.")
        }
    }

    /**
     * 내 포지션 조회
     * 인증 필수 (JWT)
     */
    @GetMapping("/swap/my-shares/{questionId}")
    fun getMyShares(
        @PathVariable questionId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<MySharesSnapshot>> {
        val memberId = httpRequest.authenticatedMemberId()
        val response = swapService.getMyShares(memberId, questionId)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 가격 히스토리 조회
     * 인증 불필요
     */
    @GetMapping("/swap/price-history/{questionId}")
    fun getPriceHistory(
        @PathVariable questionId: Long,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<ApiEnvelope<List<PricePointResponse>>> {
        return try {
            val response = swapService.getPriceHistory(questionId, limit)
            ResponseEntity.ok(ApiEnvelope.ok(response))
        } catch (e: IllegalArgumentException) {
            throw NotFoundException(e.message ?: "Question not found.")
        }
    }

    /**
     * 공개 스왑 내역 조회
     * 인증 불필요
     */
    @GetMapping("/swap/history/{questionId}")
    fun getSwapHistory(
        @PathVariable questionId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<ApiEnvelope<List<SwapHistoryResponse>>> {
        val response = swapService.getSwapHistory(questionId, limit)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 내 스왑 내역 조회
     * 인증 필수
     */
    @GetMapping("/swap/my-history/{questionId}")
    fun getMySwapHistory(
        @PathVariable questionId: Long,
        @RequestParam(defaultValue = "50") limit: Int,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<List<SwapHistoryResponse>>> {
        val memberId = httpRequest.authenticatedMemberId()
        val response = swapService.getMySwapHistory(memberId, questionId, limit)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 풀 초기화
     * ADMIN 권한 필수
     */
    @PostMapping("/pool/seed")
    @PreAuthorize("hasRole('ADMIN')")
    fun seedPool(
        @RequestBody request: SeedPoolRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<SeedPoolResponse>> {
        val role = httpRequest.authenticatedRole()
        if (role != "ADMIN") {
            throw ForbiddenException("Administrator privileges required.")
        }

        val response = swapService.seedPool(request)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }
}
