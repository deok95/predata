package com.predata.backend.repository

import com.predata.backend.domain.TransactionHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionHistoryRepository : JpaRepository<TransactionHistory, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<TransactionHistory>
    fun findByMemberIdAndTypeOrderByCreatedAtDesc(memberId: Long, type: String, pageable: Pageable): Page<TransactionHistory>
}
