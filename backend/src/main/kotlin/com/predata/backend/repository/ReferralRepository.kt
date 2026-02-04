package com.predata.backend.repository

import com.predata.backend.domain.Referral
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReferralRepository : JpaRepository<Referral, Long> {
    fun findByReferrerId(referrerId: Long): List<Referral>
    fun findByRefereeId(refereeId: Long): Optional<Referral>
    fun countByReferrerId(referrerId: Long): Long
    fun existsByRefereeId(refereeId: Long): Boolean
}
