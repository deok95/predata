package com.predata.backend.repository

import com.predata.backend.domain.VoteRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRecordRepository : JpaRepository<VoteRecord, Long> {

    fun findByVoteId(voteId: Long): VoteRecord?

    fun findByMemberId(memberId: Long): List<VoteRecord>

    fun findByQuestionId(questionId: Long): List<VoteRecord>

    fun existsByVoteId(voteId: Long): Boolean

    fun findByTxHash(txHash: String): VoteRecord?
}
