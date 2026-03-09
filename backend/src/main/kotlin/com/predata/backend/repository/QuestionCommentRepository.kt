package com.predata.backend.repository

import com.predata.backend.domain.QuestionComment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface CommentCountRow {
    fun getQuestionId(): Long
    fun getCount(): Long
}

@Repository
interface QuestionCommentRepository : JpaRepository<QuestionComment, Long> {
    fun findByQuestionIdOrderByCreatedAtDesc(questionId: Long, pageable: Pageable): Page<QuestionComment>

    @Query(
        "SELECT qc.questionId AS questionId, COUNT(qc) AS count " +
        "FROM QuestionComment qc WHERE qc.questionId IN :ids GROUP BY qc.questionId"
    )
    fun countsByQuestionIds(@Param("ids") ids: List<Long>): List<CommentCountRow>
}
