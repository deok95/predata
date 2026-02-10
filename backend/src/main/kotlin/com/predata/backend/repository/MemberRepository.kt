package com.predata.backend.repository

import com.predata.backend.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByGoogleId(googleId: String): Optional<Member>
    fun findByWalletAddress(walletAddress: String): Optional<Member>
    fun findByReferralCode(referralCode: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
    fun existsByGoogleId(googleId: String): Boolean
    fun findByIsBannedTrue(): List<Member>
    fun findBySignupIp(ip: String): List<Member>
    fun findByLastIp(ip: String): List<Member>
}
