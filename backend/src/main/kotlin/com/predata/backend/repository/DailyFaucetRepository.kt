package com.predata.backend.repository

import com.predata.backend.domain.DailyFaucet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface DailyFaucetRepository : JpaRepository<DailyFaucet, Long> {

    fun findByMemberIdAndResetDate(memberId: Long, resetDate: LocalDate): Optional<DailyFaucet>
}
