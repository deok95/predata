package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "referrals")
data class Referral(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "referral_id")
    val id: Long? = null,

    @Column(name = "referrer_id", nullable = false)
    val referrerId: Long,

    @Column(name = "referee_id", nullable = false)
    val refereeId: Long,

    @Column(name = "referral_code", nullable = false, length = 12)
    val referralCode: String,

    @Column(name = "referrer_reward")
    val referrerReward: Long = 500,

    @Column(name = "referee_reward")
    val refereeReward: Long = 500,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
