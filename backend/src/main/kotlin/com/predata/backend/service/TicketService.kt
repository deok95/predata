package com.predata.backend.service

import com.predata.backend.domain.DailyTicket
import com.predata.backend.repository.DailyTicketRepository
import com.predata.backend.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class TicketService(
    private val dailyTicketRepository: DailyTicketRepository,
    private val memberRepository: MemberRepository,
    private val dailyTicketFactory: DailyTicketFactory,
) {
    private val logger = LoggerFactory.getLogger(TicketService::class.java)

    companion object {
        private const val DEFAULT_MAX_TICKETS = 5
        private const val ADMIN_MAX_TICKETS = 100
    }

    /**
     * 오늘 날짜(UTC) 기준 티켓 조회 또는 생성.
     *
     * 동시성 안전: 현재 트랜잭션에서 먼저 SELECT를 시도한다.
     * 없으면 DailyTicketFactory(REQUIRES_NEW)로 INSERT를 격리하므로
     * 경쟁 INSERT의 DataIntegrityViolationException이 외부 트랜잭션을 오염시키지 않는다.
     */
    @Transactional
    fun getOrCreateTodayTicket(memberId: Long): DailyTicket {
        val today = LocalDate.now(ZoneOffset.UTC)
        val existing = dailyTicketRepository.findByMemberIdAndResetDate(memberId, today)
        if (existing.isPresent) return existing.get()

        return try {
            dailyTicketFactory.getOrCreate(memberId, today, getMaxTickets(memberId))
        } catch (e: DataIntegrityViolationException) {
            // 경쟁 스레드가 먼저 INSERT 완료 → REQUIRES_NEW 롤백, 외부 트랜잭션은 정상
            logger.debug("Race on DailyTicket insert for memberId={}, selecting existing row", memberId)
            dailyTicketRepository.findByMemberIdAndResetDate(memberId, today)
                .orElseThrow { IllegalStateException("DailyTicket not found after race for memberId=$memberId") }
        }
    }

    /**
     * 티켓 1개 차감. 비관적 쓰기 락으로 lost-update 방지.
     * 반환값: 차감 성공 여부 (false = 티켓 소진)
     */
    @Transactional
    fun consumeTicket(memberId: Long): Boolean {
        val today = LocalDate.now(ZoneOffset.UTC)
        // 비관적 쓰기 락 — 동일 멤버의 동시 차감을 직렬화
        val ticket = dailyTicketRepository.findByMemberIdAndResetDateForUpdate(memberId, today)
            .orElseGet { getOrCreateTodayTicket(memberId) }

        if (ticket.remainingCount <= 0) {
            return false
        }

        ticket.remainingCount -= 1
        dailyTicketRepository.save(ticket)
        return true
    }

    /**
     * 남은 티켓 개수 조회.
     *
     * 현재 트랜잭션 내에서 직접 조회한다. Hibernate AUTO flush 덕분에
     * 같은 트랜잭션에서 consumeTicket()이 수행한 미커밋 차감도 정확히 반영된다.
     * (REQUIRES_NEW를 우회하여 uncommitted decrement를 볼 수 있음)
     */
    @Transactional
    fun getRemainingTickets(memberId: Long): Int {
        val today = LocalDate.now(ZoneOffset.UTC)
        return dailyTicketRepository.findByMemberIdAndResetDate(memberId, today)
            .map { it.remainingCount }
            .orElseGet { getMaxTickets(memberId) }
    }

    @Transactional(readOnly = true)
    fun getMaxTickets(memberId: Long): Int {
        val role = memberRepository.findById(memberId).orElse(null)?.role?.uppercase()
        return if (role == "ADMIN") ADMIN_MAX_TICKETS else DEFAULT_MAX_TICKETS
    }
}
