package com.predata.backend.repository

import com.predata.backend.domain.Choice
import com.predata.backend.domain.VoteCommit
import com.predata.backend.domain.VoteCommitStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface VoteCommitRepository : JpaRepository<VoteCommit, Long> {
    /**
     * 특정 회원의 특정 질문에 대한 투표 커밋 조회
     */
    fun findByMemberIdAndQuestionId(memberId: Long, questionId: Long): VoteCommit?

    /**
     * 중복 투표 방지: 이미 커밋했는지 확인
     */
    fun existsByMemberIdAndQuestionId(memberId: Long, questionId: Long): Boolean

    /**
     * 특정 질문의 특정 상태 투표 개수
     */
    fun countByQuestionIdAndStatus(questionId: Long, status: VoteCommitStatus): Long

    /**
     * 일일 투표 개수 (UTC 기준)
     * - 일일 한도 체크용
     */
    @Query("SELECT COUNT(v) FROM VoteCommit v WHERE v.memberId = :memberId AND v.committedAt BETWEEN :start AND :end")
    fun countByMemberIdAndCommittedAtBetween(
        memberId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): Long

    /**
     * 특정 질문의 특정 선택 투표 개수
     */
    fun countByQuestionIdAndRevealedChoice(questionId: Long, choice: Choice): Long
}
