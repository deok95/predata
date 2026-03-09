package com.predata.backend.repository

import com.predata.backend.domain.DraftStatus
import com.predata.backend.domain.QuestionDraftSession
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface QuestionDraftSessionRepository : JpaRepository<QuestionDraftSession, Long> {

    fun findByDraftId(draftId: String): QuestionDraftSession?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT d FROM QuestionDraftSession d WHERE d.draftId = :draftId")
    fun findByDraftIdWithLock(@Param("draftId") draftId: String): QuestionDraftSession?

    /** OPEN draft 점유 조회 — active_member_id UNIQUE 컬럼 기반 */
    fun findByActiveMemberId(memberId: Long): QuestionDraftSession?

    /** 회원의 유효한 OPEN draft 조회 (만료 전) */
    @Query(
        """
        SELECT d FROM QuestionDraftSession d
        WHERE d.memberId = :memberId
          AND d.status   = :status
          AND d.expiresAt > :now
        ORDER BY d.createdAt DESC
        """
    )
    fun findOpenDraft(
        @Param("memberId") memberId: Long,
        @Param("status") status: DraftStatus,
        @Param("now") now: LocalDateTime,
    ): QuestionDraftSession?
}
