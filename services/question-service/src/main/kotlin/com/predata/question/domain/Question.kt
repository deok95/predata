package com.predata.question.domain

import com.predata.common.domain.QuestionStatus
import com.predata.common.domain.FinalResult
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "questions")
data class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    val id: Long? = null,

    @Column(nullable = false, length = 500)
    val title: String,

    @Column(length = 100)
    val category: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: QuestionStatus = QuestionStatus.OPEN,

    @Column(name = "total_bet_pool")
    var totalBetPool: Long = 0,

    @Column(name = "yes_bet_pool")
    var yesBetPool: Long = 0,

    @Column(name = "no_bet_pool")
    var noBetPool: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "final_result", length = 20)
    var finalResult: FinalResult? = null,

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
