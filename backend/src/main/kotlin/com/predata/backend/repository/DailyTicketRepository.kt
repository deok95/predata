package com.predata.backend.repository

import com.predata.backend.domain.DailyTicket
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface DailyTicketRepository : JpaRepository<DailyTicket, Long> {

    fun findByMemberIdAndResetDate(memberId: Long, resetDate: LocalDate): Optional<DailyTicket>

    /**
     * 티켓 차감용 비관적 쓰기 락 조회.
     * 동일 행에 대한 동시 차감이 직렬화되어 lost-update를 방지한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM DailyTicket t WHERE t.memberId = :memberId AND t.resetDate = :resetDate")
    fun findByMemberIdAndResetDateForUpdate(memberId: Long, resetDate: LocalDate): Optional<DailyTicket>

    fun findByMemberId(memberId: Long): List<DailyTicket>
}
