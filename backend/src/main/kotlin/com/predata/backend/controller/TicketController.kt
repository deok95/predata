package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.service.TicketService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 투표 티켓 컨트롤러
 * - GET /api/tickets/status: 남은 티켓 조회
 */
@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val ticketService: TicketService
) {

    /**
     * 티켓 상태 조회
     * - JWT 인증 필수
     * - 남은 티켓 수, 최대 티켓 수, 리셋 시간 반환
     */
    @GetMapping("/status")
    fun getStatus(
        httpRequest: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        val remainingTickets = ticketService.getRemainingTickets(authenticatedMemberId)
        val resetAt = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .toString()

        return ResponseEntity.ok(
            mapOf(
                "remainingTickets" to remainingTickets,
                "maxTickets" to 5,
                "resetAt" to resetAt
            )
        )
    }
}
