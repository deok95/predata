package com.predata.backend.repository

import com.predata.backend.domain.BetRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BetRecordRepository : JpaRepository<BetRecord, Long> {

    fun findByBetId(betId: Long): BetRecord?

    fun findByMemberId(memberId: Long): List<BetRecord>

    fun findByQuestionId(questionId: Long): List<BetRecord>

    fun existsByBetId(betId: Long): Boolean

    fun findByTxHash(txHash: String): BetRecord?
}
