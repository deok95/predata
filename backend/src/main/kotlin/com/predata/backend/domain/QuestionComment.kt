package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "question_comments",
    indexes = [
        Index(name = "idx_qcomments_question_created", columnList = "question_id, created_at"),
        Index(name = "idx_qcomments_member_created", columnList = "member_id, created_at"),
        Index(name = "idx_qcomments_parent", columnList = "parent_comment_id")
    ]
)
data class QuestionComment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "parent_comment_id")
    val parentCommentId: Long? = null,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
