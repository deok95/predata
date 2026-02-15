package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Commit-Reveal 투표 엔티티
 * - Commit 단계: commitHash 저장 (SHA-256)
 * - Reveal 단계: choice + salt 검증 후 revealedChoice 저장
 */
@Entity
@Table(
    name = "vote_commits",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_vote_commit_member_question",
            columnNames = ["question_id", "member_id"]
        )
    ],
    indexes = [
        Index(name = "idx_vote_commit_status", columnList = "status, committed_at"),
        Index(name = "idx_vote_commit_question", columnList = "question_id")
    ]
)
data class VoteCommit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_commit_id")
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "commit_hash", nullable = false, length = 64)
    val commitHash: String,  // SHA-256 해시 (64자 hex)

    @Column(nullable = false, length = 64)
    var salt: String,  // 클라이언트 생성 salt (검증용)

    @Enumerated(EnumType.STRING)
    @Column(name = "revealed_choice", length = 3)
    var revealedChoice: Choice? = null,  // Reveal 후 저장 (YES or NO)

    @Column(name = "committed_at", nullable = false, updatable = false)
    val committedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "revealed_at")
    var revealedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: VoteCommitStatus = VoteCommitStatus.COMMITTED,

    @Version
    var version: Long? = null  // 낙관적 잠금
)
