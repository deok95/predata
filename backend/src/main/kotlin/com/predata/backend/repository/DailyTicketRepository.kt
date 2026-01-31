package com.predata.backend.repository

import com.predata.backend.domain.DailyTicket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface DailyTicketRepository : JpaRepository<DailyTicket, Long> {
    
    fun findByMemberIdAndResetDate(memberId: Long, resetDate: LocalDate): Optional<DailyTicket>
    
    fun findByMemberId(memberId: Long): List<DailyTicket>
}
