package com.predata.backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "members")
data class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    val id: Long? = null,

    @Version
    var version: Long = 0L,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "google_id", unique = true, length = 255)
    var googleId: String? = null, // Google OAuth User ID (sub claim)

    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Column(name = "wallet_address", unique = true, length = 42)
    var walletAddress: String? = null, // 지갑 주소 (0x...)

    @Column(name = "country_code", nullable = false, length = 2)
    val countryCode: String,

    @Column(name = "job_category", length = 50)
    val jobCategory: String? = null,

    @Column(name = "age_group")
    val ageGroup: Int? = null,

    @Column(name = "gender", length = 10)
    @Enumerated(EnumType.STRING)
    val gender: Gender? = null,

    @Column(name = "birth_date")
    val birthDate: LocalDate? = null,

    @Column(length = 20)
    var tier: String = "BRONZE",

    @Column(name = "tier_weight", precision = 3, scale = 2)
    var tierWeight: BigDecimal = BigDecimal("1.00"),

    @Column(name = "level", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
    var level: Int = 1, // 레벨 (1~5, 보상 가중치 계산용)

    @Column(name = "point_balance", precision = 18, scale = 6, nullable = false, columnDefinition = "DECIMAL(18,6) NOT NULL DEFAULT 0.000000")
    var pointBalance: BigDecimal = BigDecimal.ZERO, // 포인트 잔액 (리워드 수령)

    @Column(name = "accuracy_score")
    var accuracyScore: Int = 0, // 정확도 점수 (누적)

    @Column(name = "total_predictions")
    var totalPredictions: Int = 0, // 총 예측 횟수

    @Column(name = "correct_predictions")
    var correctPredictions: Int = 0, // 정확한 예측 횟수

    @Column(name = "usdc_balance", precision = 18, scale = 6)
    var usdcBalance: BigDecimal = BigDecimal.ZERO,

    @Column(length = 20)
    var role: String = "USER",  // USER 또는 ADMIN

    // === 레퍼럴 시스템 ===
    @Column(name = "referral_code", unique = true, length = 10)
    var referralCode: String? = null,

    @Column(name = "referred_by")
    var referredBy: Long? = null,

    // === 투표 패스 ===
    @Column(name = "has_voting_pass", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    var hasVotingPass: Boolean = false,

    // === 어뷰징 방지 ===
    @Column(name = "is_banned")
    var isBanned: Boolean = false,

    @Column(name = "ban_reason", length = 255)
    var banReason: String? = null,

    @Column(name = "banned_at")
    var bannedAt: LocalDateTime? = null,

    @Column(name = "signup_ip", length = 45)
    var signupIp: String? = null,

    @Column(name = "last_ip", length = 45)
    var lastIp: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}
