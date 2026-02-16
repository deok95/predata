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

    /**
     * 질문 ID와 상태로 투표 조회 (memberId 오름차순 정렬)
     * - 결정론적 계산을 위한 정렬 보장
     */
    fun findByQuestionIdAndStatusOrderByMemberIdAsc(questionId: Long, status: VoteCommitStatus): List<VoteCommit>

    /**
     * 특정 회원이 투표한 다른 질문 개수 (시빌 가드용)
     */
    @Query("SELECT COUNT(DISTINCT v.questionId) FROM VoteCommit v WHERE v.memberId = :memberId")
    fun countDistinctQuestionsByMemberId(memberId: Long): Long

    /**
     * 특정 질문에 투표한 회원들의 IP 주소 조회 (시빌 탐지용)
     */
    @Query("""
        SELECT m.lastIp, COUNT(DISTINCT v.memberId) as memberCount
        FROM VoteCommit v
        JOIN Member m ON v.memberId = m.id
        WHERE v.questionId = :questionId AND m.lastIp IS NOT NULL
        GROUP BY m.lastIp
        HAVING COUNT(DISTINCT v.memberId) >= :threshold
    """)
    fun findSuspiciousIpsByQuestionId(questionId: Long, threshold: Int): List<Array<Any>>

    /**
     * 특정 회원의 모든 투표 선택 조회 (패턴 탐지용)
     */
    @Query("SELECT v.revealedChoice FROM VoteCommit v WHERE v.memberId = :memberId AND v.revealedChoice IS NOT NULL")
    fun findRevealedChoicesByMemberId(memberId: Long): List<Choice>

    /**
     * 특정 상태의 투표 개수 (대시보드용)
     */
    fun countByStatus(status: VoteCommitStatus): Long
}
