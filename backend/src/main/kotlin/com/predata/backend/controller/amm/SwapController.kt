package com.predata.backend.controller.amm

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import com.predata.backend.dto.amm.*
import com.predata.backend.service.amm.SwapService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
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
    ): ResponseEntity<Map<String, Any>> {
        // JWT에서 인증된 memberId 가져오기
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf(
                    "success" to false,
                    "message" to "Authentication required."
                )
            )

        return try {
            val response = retryableSwapFacade.executeSwapWithRetry(memberId, request)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: OptimisticLockingFailureException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "success" to false,
                    "message" to "Trading is congested. Please try again later."
                )
            )
        } catch (e: ObjectOptimisticLockingFailureException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "success" to false,
                    "message" to "Trading is congested. Please try again later."
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Invalid request.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
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
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val response = swapService.simulateSwap(questionId, action, outcome, amount)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Invalid request.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
        }
    }

    /**
     * 풀 상태 조회
     * 인증 불필요
     */
    @GetMapping("/pool/{questionId}")
    fun getPoolState(
        @PathVariable questionId: Long
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val response = swapService.getPoolState(questionId)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Market pool not found.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
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
    ): ResponseEntity<Map<String, Any>> {
        // JWT에서 인증된 memberId 가져오기
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf(
                    "success" to false,
                    "message" to "Authentication required."
                )
            )

        return try {
            val response = swapService.getMyShares(memberId, questionId)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
        }
    }

    /**
     * 가격 히스토리 조회
     * 인증 불필요
     */
    @GetMapping("/swap/price-history/{questionId}")
    fun getPriceHistory(
        @PathVariable questionId: Long,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val response = swapService.getPriceHistory(questionId, limit)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Question not found.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
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
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val response = swapService.getSwapHistory(questionId, limit)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
        }
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
    ): ResponseEntity<Map<String, Any>> {
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf(
                    "success" to false,
                    "message" to "Authentication required."
                )
            )

        return try {
            val response = swapService.getMySwapHistory(memberId, questionId, limit)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
        }
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
    ): ResponseEntity<Map<String, Any>> {
        // ADMIN 권한 체크 (이미 @PreAuthorize로 처리되지만 명시적 확인)
        val role = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_ROLE) as? String
        if (role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                mapOf(
                    "success" to false,
                    "message" to "Administrator privileges required."
                )
            )
        }

        return try {
            val response = swapService.seedPool(request)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to response
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Invalid request.")
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Pool already exists.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "Server error occurred."
                )
            )
        }
    }
}
