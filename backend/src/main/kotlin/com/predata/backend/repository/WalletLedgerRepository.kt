package com.predata.backend.repository

import com.predata.backend.domain.WalletLedger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletLedgerRepository : JpaRepository<WalletLedger, Long> {
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<WalletLedger>

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<WalletLedger>

    fun findByMemberIdOrderByCreatedAtAsc(memberId: Long, pageable: Pageable): Page<WalletLedger>
}
