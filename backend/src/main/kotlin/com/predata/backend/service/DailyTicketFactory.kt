package com.predata.backend.service

import com.predata.backend.domain.DailyTicket
import com.predata.backend.repository.DailyTicketRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * DailyTicket 생성 전용 컴포넌트.
 *
 * REQUIRES_NEW 전파를 사용하여 INSERT 실패(유니크 충돌) 시
 * 외부 트랜잭션을 rollback-only 상태로 오염시키지 않고 안전하게 격리한다.
 * TicketService 내부에서 직접 REQUIRES_NEW를 호출하면 Spring 프록시를 우회하므로
 * 반드시 별도 빈으로 분리해야 한다.
 */
@Component
class DailyTicketFactory(private val dailyTicketRepository: DailyTicketRepository) {

    private val logger = LoggerFactory.getLogger(DailyTicketFactory::class.java)

    /**
     * 오늘 티켓이 없으면 INSERT, 있으면 SELECT 반환.
     * 동시 INSERT 경쟁 시 패배 트랜잭션은 DataIntegrityViolationException으로 즉시 롤백된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun getOrCreate(memberId: Long, today: LocalDate, defaultCount: Int): DailyTicket {
        return dailyTicketRepository.findByMemberIdAndResetDate(memberId, today)
            .orElseGet {
                logger.debug("Creating daily ticket: memberId={}, date={}", memberId, today)
                dailyTicketRepository.saveAndFlush(
                    DailyTicket(memberId = memberId, remainingCount = defaultCount, resetDate = today)
                )
            }
    }
}
