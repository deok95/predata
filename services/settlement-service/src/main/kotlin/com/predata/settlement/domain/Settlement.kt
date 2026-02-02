package com.predata.settlement.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "settlements")
data class Settlement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false, unique = true)
    val questionId: Long,

    @Column(name = "final_result", nullable = false)
    val finalResult: String, // YES or NO

    @Column(name = "total_bets")
    val totalBets: Int = 0,

    @Column(name = "total_winners")
    val totalWinners: Int = 0,

    @Column(name = "total_payout")
    val totalPayout: Long = 0,

    @Column(name = "settled_at", nullable = false)
    val settledAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "rewards")
data class Reward(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Column(name = "reward_type", nullable = false)
    val rewardType: String, // PAYOUT, VOTER_REWARD

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
