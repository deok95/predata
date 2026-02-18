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
                    "message" to "인증이 필요합니다."
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
                    "message" to "거래가 혼잡합니다. 잠시 후 다시 시도해주세요."
                )
            )
        } catch (e: ObjectOptimisticLockingFailureException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "success" to false,
                    "message" to "거래가 혼잡합니다. 잠시 후 다시 시도해주세요."
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "잘못된 요청입니다.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "서버 오류가 발생했습니다."
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
                    "message" to (e.message ?: "잘못된 요청입니다.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "서버 오류가 발생했습니다."
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
                    "message" to (e.message ?: "마켓 풀을 찾을 수 없습니다.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "서버 오류가 발생했습니다."
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
                    "message" to "인증이 필요합니다."
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
                    "message" to "서버 오류가 발생했습니다."
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
                    "message" to (e.message ?: "질문을 찾을 수 없습니다.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "서버 오류가 발생했습니다."
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
                    "message" to "관리자 권한이 필요합니다."
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
                    "message" to (e.message ?: "잘못된 요청입니다.")
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "풀이 이미 존재합니다.")
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "message" to "서버 오류가 발생했습니다."
                )
            )
        }
    }
}
