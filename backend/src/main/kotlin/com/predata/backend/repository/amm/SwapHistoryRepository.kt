package com.predata.backend.repository.amm

import com.predata.backend.domain.SwapAction
import com.predata.backend.domain.SwapHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SwapHistoryRepository : JpaRepository<SwapHistory, Long> {

    fun findByQuestionIdOrderByCreatedAtDesc(questionId: Long, pageable: Pageable): Page<SwapHistory>

    fun findByQuestionIdOrderByCreatedAtAsc(questionId: Long, pageable: Pageable): Page<SwapHistory>

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<SwapHistory>

    fun findByMemberIdAndQuestionIdOrderByCreatedAtDesc(
        memberId: Long,
        questionId: Long,
        pageable: Pageable
    ): Page<SwapHistory>

    @Query("""
        SELECT sh FROM SwapHistory sh
        WHERE sh.questionId = :questionId
        AND sh.createdAt >= :since
        ORDER BY sh.createdAt DESC
    """)
    fun findRecentSwaps(questionId: Long, since: LocalDateTime): List<SwapHistory>

    @Query("""
        SELECT COUNT(sh) FROM SwapHistory sh
        WHERE sh.questionId = :questionId
        AND sh.action = :action
    """)
    fun countByQuestionIdAndAction(questionId: Long, action: SwapAction): Long
}
