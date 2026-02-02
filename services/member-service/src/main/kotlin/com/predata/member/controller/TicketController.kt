package com.predata.member.controller

import com.predata.common.dto.ApiResponse
import com.predata.member.dto.TicketStatusDto
import com.predata.member.service.TicketService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val ticketService: TicketService
) {

    /**
     * 회원의 오늘 티켓 상태 조회
     */
    @GetMapping("/{memberId}")
    fun getTicketStatus(@PathVariable memberId: Long): ApiResponse<TicketStatusDto> {
        val ticket = ticketService.getOrCreateTodayTicket(memberId)

        return ApiResponse(
            success = true,
            data = TicketStatusDto(
                remainingCount = ticket.remainingCount,
                resetDate = ticket.resetDate.toString()
            )
        )
    }

    /**
     * 티켓 차감 (테스트용 - 실제로는 BetService에서 호출)
     */
    @PostMapping("/{memberId}/consume")
    fun consumeTicket(@PathVariable memberId: Long): ApiResponse<TicketStatusDto> {
        val success = ticketService.consumeTicket(memberId)

        if (!success) {
            return ApiResponse(
                success = false,
                data = null,
                message = "티켓이 부족합니다."
            )
        }

        val ticket = ticketService.getOrCreateTodayTicket(memberId)
        return ApiResponse(
            success = true,
            data = TicketStatusDto(
                remainingCount = ticket.remainingCount,
                resetDate = ticket.resetDate.toString()
            )
        )
    }
}
