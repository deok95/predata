package com.predata.backend.repository

import com.predata.backend.domain.Choice
import com.predata.backend.domain.VoteRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRecordRepository : JpaRepository<VoteRecord, Long> {

    // ── 단건 조회 ────────────────────────────────────────────────────────────

    fun findByVoteId(voteId: Long): VoteRecord?

    fun findByMemberIdAndQuestionId(memberId: Long, questionId: Long): VoteRecord?

    fun findByTxHash(txHash: String): VoteRecord?

    // ── 존재 여부 ─────────────────────────────────────────────────────────────

    fun existsByVoteId(voteId: Long): Boolean

    /** 중복 투표 방지: 이미 투표했는지 확인 */
    fun existsByMemberIdAndQuestionId(memberId: Long, questionId: Long): Boolean

    // ── 목록 조회 ─────────────────────────────────────────────────────────────

    fun findByMemberId(memberId: Long): List<VoteRecord>

    fun findByQuestionId(questionId: Long): List<VoteRecord>

    // ── 집계 ─────────────────────────────────────────────────────────────────

    /** YES 또는 NO 선택별 투표 수 (query 서비스용) */
    fun countByQuestionIdAndChoice(questionId: Long, choice: Choice): Long
}
