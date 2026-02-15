package com.predata.backend.dto

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.NotBlank

// 투표 요청
data class VoteRequest(
    @field:NotNull(message = "질문 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 질문 ID입니다.")
    val questionId: Long,

    @field:NotNull(message = "선택(YES/NO)은 필수입니다.")
    val choice: Choice,

    val latencyMs: Int? = null
)

// 베팅 요청
data class BetRequest(
    @field:NotNull(message = "질문 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 질문 ID입니다.")
    val questionId: Long,

    @field:NotNull(message = "선택(YES/NO)은 필수입니다.")
    val choice: Choice,

    @field:NotNull(message = "베팅 금액은 필수입니다.")
    @field:Min(value = 1, message = "최소 베팅 금액은 1P입니다.")
    @field:Max(value = 100, message = "최대 베팅 금액은 100P입니다.")
    val amount: Long,

    val latencyMs: Int? = null
)

// 베팅 판매 요청
data class SellBetRequest(
    @field:NotNull(message = "베팅 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 베팅 ID입니다.")
    val betId: Long
)

// 베팅 판매 응답
data class SellBetResponse(
    val success: Boolean,
    val message: String? = null,
    val originalBetAmount: Long? = null,
    val refundAmount: Long? = null,
    val profit: Long? = null,  // refundAmount - originalBetAmount
    val newPoolYes: Long? = null,
    val newPoolNo: Long? = null,
    val newPoolTotal: Long? = null,
    val sellActivityId: Long? = null
)

// 활동 응답
data class ActivityResponse(
    val success: Boolean,
    val message: String? = null,
    val activityId: Long? = null,
    val remainingTickets: Int? = null
)

// 질문 조회 응답
data class QuestionResponse(
    val id: Long,
    val title: String,
    val category: String?,
    val status: String,
    val type: String,
    val finalResult: String?,
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val yesPercentage: Double,
    val noPercentage: Double,
    val sourceUrl: String?,
    val disputeDeadline: String?,
    val votingEndAt: String,
    val bettingStartAt: String,
    val bettingEndAt: String,
    val expiredAt: String,
    val createdAt: String
)

// 티켓 현황 응답
data class TicketStatusResponse(
    val remainingCount: Int,
    val resetDate: String
)

// === 인증 요청 DTO ===

// Step 1: 이메일로 인증 코드 발송
data class SendCodeRequest(
    val email: String
)

// Step 2: 인증 코드 검증만 (회원 생성 안 함)
data class VerifyCodeRequest(
    val email: String,
    val code: String
)

// Step 3: 비밀번호 설정 및 회원 생성
data class CompleteSignupRequest(
    val email: String,
    val code: String,
    val password: String,
    val passwordConfirm: String
)

// 로그인 요청
data class LoginRequest(
    val email: String,
    val password: String
)

// === 어드민 질문 생성 DTO ===

data class AdminCreateQuestionRequest(
    @field:NotNull(message = "질문 제목은 필수입니다.")
    val title: String,

    @field:NotNull(message = "질문 타입은 필수입니다.")
    val type: com.predata.backend.domain.QuestionType,

    @field:NotNull(message = "시장 타입은 필수입니다.")
    val marketType: com.predata.backend.domain.MarketType = com.predata.backend.domain.MarketType.VERIFIABLE,

    @field:NotBlank(message = "정산 규칙은 필수입니다.")
    val resolutionRule: String,

    val resolutionSource: String? = null,

    val resolveAt: String? = null, // ISO 8601 형식

    val disputeUntil: String? = null, // ISO 8601 형식

    val category: String? = null,

    @field:Min(value = 60, message = "투표 기간은 최소 60초입니다.")
    val votingDuration: Long = 3600, // 기본 1시간 (초 단위)

    @field:Min(value = 60, message = "베팅 기간은 최소 60초입니다.")
    val bettingDuration: Long = 3600 // 기본 1시간 (초 단위)
)

// 관리자 결과 입력 요청
data class SetQuestionResultRequest(
    @field:NotNull(message = "결과(YES/NO)는 필수입니다.")
    val result: String // "YES" or "NO"
)

// ============================================================
// Google OAuth DTOs
// ============================================================

// Google OAuth 로그인 요청
data class GoogleAuthRequest(
    @field:NotBlank(message = "Google token is required")
    val googleToken: String,         // Google에서 받은 ID Token
    val countryCode: String? = null, // 추가 정보 (선택)
    val jobCategory: String? = null,
    val ageGroup: Int? = null
)

// Google OAuth 로그인 응답
data class GoogleAuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,           // JWT 토큰
    val memberId: Long? = null,
    val needsAdditionalInfo: Boolean = false  // 추가 정보 입력 필요 여부
)

// Google OAuth 회원가입 완료 요청 (추가 정보)
data class CompleteGoogleRegistrationRequest(
    @field:NotBlank(message = "Google ID is required")
    val googleId: String,
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Country code is required")
    val countryCode: String,
    val jobCategory: String? = null,
    val ageGroup: Int? = null
)
