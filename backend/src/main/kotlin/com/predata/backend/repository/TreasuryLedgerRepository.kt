package com.predata.backend.repository

import com.predata.backend.domain.TreasuryLedger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TreasuryLedgerRepository : JpaRepository<TreasuryLedger, Long> {
    fun findByTxType(txType: String, pageable: Pageable): Page<TreasuryLedger>

    fun findByTxTypeOrderByCreatedAtDesc(txType: String, pageable: Pageable): Page<TreasuryLedger>
    fun findByTxTypeOrderByCreatedAtAsc(txType: String, pageable: Pageable): Page<TreasuryLedger>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<TreasuryLedger>
    fun findAllByOrderByCreatedAtAsc(pageable: Pageable): Page<TreasuryLedger>
}
