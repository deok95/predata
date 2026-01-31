package com.predata.backend.repository

import com.predata.backend.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByWalletAddress(walletAddress: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
}
