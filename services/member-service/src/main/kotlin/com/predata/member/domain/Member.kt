package com.predata.member.domain

import com.predata.common.domain.Tier
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "members")
data class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "wallet_address", unique = true, length = 42)
    var walletAddress: String? = null, // 지갑 주소 (0x...)

    @Column(name = "country_code", nullable = false, length = 2)
    val countryCode: String,

    @Column(name = "job_category", length = 50)
    val jobCategory: String? = null,

    @Column(name = "age_group")
    val ageGroup: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var tier: Tier = Tier.BRONZE,

    @Column(name = "tier_weight", precision = 3, scale = 2)
    var tierWeight: BigDecimal = BigDecimal("1.00"),

    @Column(name = "accuracy_score")
    var accuracyScore: Int = 0, // 정확도 점수 (누적)

    @Column(name = "total_predictions")
    var totalPredictions: Int = 0, // 총 예측 횟수

    @Column(name = "correct_predictions")
    var correctPredictions: Int = 0, // 정확한 예측 횟수

    @Column(name = "point_balance")
    var pointBalance: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
