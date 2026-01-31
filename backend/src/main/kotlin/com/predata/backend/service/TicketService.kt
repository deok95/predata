package com.predata.backend.service

import com.predata.backend.domain.DailyTicket
import com.predata.backend.repository.DailyTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TicketService(
    private val dailyTicketRepository: DailyTicketRepository
) {

    /**
     * 오늘 날짜 기준 티켓 조회 또는 생성
     */
    @Transactional
    fun getOrCreateTodayTicket(memberId: Long): DailyTicket {
        val today = LocalDate.now()
        
        return dailyTicketRepository.findByMemberIdAndResetDate(memberId, today)
            .orElseGet {
                dailyTicketRepository.save(
                    DailyTicket(
                        memberId = memberId,
                        remainingCount = 5,
                        resetDate = today
                    )
                )
            }
    }

    /**
     * 티켓 1개 차감 (5-Lock 체크)
     */
    @Transactional
    fun consumeTicket(memberId: Long): Boolean {
        val ticket = getOrCreateTodayTicket(memberId)
        
        if (ticket.remainingCount <= 0) {
            return false // 티켓 소진
        }
        
        ticket.remainingCount -= 1
        dailyTicketRepository.save(ticket)
        return true
    }

    /**
     * 남은 티켓 개수 조회
     */
    @Transactional(readOnly = true)
    fun getRemainingTickets(memberId: Long): Int {
        return getOrCreateTodayTicket(memberId).remainingCount
    }
}
