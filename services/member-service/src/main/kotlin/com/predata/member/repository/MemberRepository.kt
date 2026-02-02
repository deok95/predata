package com.predata.member.repository

import com.predata.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun existsByEmail(email: String): Boolean
    fun existsByWalletAddress(walletAddress: String): Boolean
    fun findByEmail(email: String): Member?
    fun findByWalletAddress(walletAddress: String): Member?
}
