package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.DailyTicketRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TicketPurchaseService(
    private val memberRepository: MemberRepository,
    private val dailyTicketRepository: DailyTicketRepository,
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository
) {

    companion object {
        const val TICKET_PRICE = 100L // 티켓 1개당 100 포인트
        const val MAX_TICKET_PURCHASE = 20 // 하루 최대 20개 구매 가능
        const val DAILY_FREE_TICKETS = 5 // 매일 무료 5개
    }

    /**
     * 티켓 구매
     */
    @Transactional
    fun purchaseTickets(memberId: Long, quantity: Int): TicketPurchaseResponse {
        if (quantity <= 0 || quantity > MAX_TICKET_PURCHASE) {
            throw IllegalArgumentException("구매 가능한 티켓 수는 1~${MAX_TICKET_PURCHASE}개입니다.")
        }

        // 1. 회원 조회
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        // 2. 비용 계산
        val totalCost = TICKET_PRICE * quantity

        if (member.pointBalance < totalCost) {
            throw IllegalArgumentException("포인트가 부족합니다. (필요: ${totalCost}, 보유: ${member.pointBalance})")
        }

        // 3. 오늘 티켓 조회 또는 생성
        val today = LocalDate.now()
        val ticketOptional = dailyTicketRepository.findByMemberIdAndResetDate(memberId, today)
        val ticket = if (ticketOptional.isPresent) {
            ticketOptional.get()
        } else {
            com.predata.backend.domain.DailyTicket(
                memberId = memberId,
                remainingCount = DAILY_FREE_TICKETS,
                resetDate = today
            )
        }

        // 4. 티켓 추가
        ticket.remainingCount += quantity

        // 5. 포인트 차감
        member.pointBalance -= totalCost

        memberRepository.save(member)
        dailyTicketRepository.save(ticket)

        return TicketPurchaseResponse(
            success = true,
            purchasedQuantity = quantity,
            totalCost = totalCost,
            remainingTickets = ticket.remainingCount,
            remainingPoints = member.pointBalance,
            message = "${quantity}개의 티켓을 구매했습니다."
        )
    }

    /**
     * 티켓 구매 내역 조회
     */
    @Transactional(readOnly = true)
    fun getPurchaseHistory(memberId: Long): List<TicketPurchaseHistory> {
        // TODO: 구매 내역을 별도 테이블에 저장하면 좋지만, 
        // 일단은 현재 티켓 상태만 반환
        val tickets = dailyTicketRepository.findByMemberId(memberId)
        
        return tickets.map { ticket ->
            TicketPurchaseHistory(
                date = ticket.resetDate.toString(),
                remainingCount = ticket.remainingCount,
                used = DAILY_FREE_TICKETS - ticket.remainingCount
            )
        }
    }
}

// ===== DTOs =====

data class TicketPurchaseResponse(
    val success: Boolean,
    val purchasedQuantity: Int,
    val totalCost: Long,
    val remainingTickets: Int,
    val remainingPoints: Long,
    val message: String
)

data class TicketPurchaseHistory(
    val date: String,
    val remainingCount: Int,
    val used: Int
)
