package com.predata.backend.service

import com.predata.backend.domain.TransactionHistory
import com.predata.backend.repository.TransactionHistoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TransactionHistoryService(
    private val transactionHistoryRepository: TransactionHistoryRepository
) {

    /**
     * Record a transaction. Called AFTER the balance has been updated.
     * Uses MANDATORY propagation to ensure it participates in the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun record(
        memberId: Long,
        type: String,
        amount: BigDecimal,
        balanceAfter: BigDecimal,
        description: String,
        questionId: Long? = null,
        txHash: String? = null
    ): TransactionHistory {
        val tx = TransactionHistory(
            memberId = memberId,
            type = type,
            amount = amount,
            balanceAfter = balanceAfter,
            description = description,
            questionId = questionId,
            txHash = txHash
        )
        return transactionHistoryRepository.save(tx)
    }

    /**
     * Get paginated transaction history for a member, optionally filtered by type.
     */
    @Transactional(readOnly = true)
    fun getHistory(memberId: Long, type: String?, page: Int, size: Int): Page<TransactionHistory> {
        val pageable = PageRequest.of(page, size)
        return if (type != null) {
            transactionHistoryRepository.findByMemberIdAndTypeOrderByCreatedAtDesc(memberId, type, pageable)
        } else {
            transactionHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
        }
    }
}
