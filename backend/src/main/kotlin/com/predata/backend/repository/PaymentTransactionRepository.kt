package com.predata.backend.repository

import com.predata.backend.domain.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, Long> {
    fun existsByTxHash(txHash: String): Boolean
    fun findByTxHash(txHash: String): PaymentTransaction?
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<PaymentTransaction>
    fun findByStatusAndType(status: String, type: String): List<PaymentTransaction>
}
