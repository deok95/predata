package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.dto.PositionResponse
import com.predata.backend.service.PositionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 포지션 조회 API
 * - JWT 인증 필요
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class PositionController(
    private val positionService: PositionService
) {

    /**
     * 내 포지션 조회 (모든 질문)
     * GET /api/positions/me
     */
    @GetMapping("/positions/me")
    fun getMyPositions(
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<List<PositionResponse>>> {
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiEnvelope(
                    success = false,
                    message = "Unauthorized",
                    data = emptyList()
                )
            )

        val positions = positionService.getPositionsWithPnL(memberId)
        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = positions
            )
        )
    }

    /**
     * 특정 질문에 대한 내 포지션 조회
     * GET /api/positions/me/question/{id}
     */
    @GetMapping("/positions/me/question/{id}")
    fun getMyPositionsByQuestion(
        @PathVariable id: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<List<PositionResponse>>> {
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiEnvelope(
                    success = false,
                    message = "Unauthorized",
                    data = emptyList()
                )
            )

        val positions = positionService.getPositionsWithPnL(memberId)
            .filter { it.questionId == id }

        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = positions
            )
        )
    }
}
