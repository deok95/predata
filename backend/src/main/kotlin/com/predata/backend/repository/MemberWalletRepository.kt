package com.predata.backend.repository

import com.predata.backend.domain.MemberWallet
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberWalletRepository : JpaRepository<MemberWallet, Long> {
    fun findByMemberId(memberId: Long): MemberWallet?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM MemberWallet w WHERE w.memberId = :memberId")
    fun findByMemberIdWithLock(@Param("memberId") memberId: Long): MemberWallet?
}

