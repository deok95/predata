package com.predata.backend.dto

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

// 투표 요청
data class VoteRequest(
    @field:NotNull(message = "회원 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 회원 ID입니다.")
    val memberId: Long,

    @field:NotNull(message = "질문 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 질문 ID입니다.")
    val questionId: Long,

    @field:NotNull(message = "선택(YES/NO)은 필수입니다.")
    val choice: Choice,

    val latencyMs: Int? = null
)

// 베팅 요청
data class BetRequest(
    @field:NotNull(message = "회원 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 회원 ID입니다.")
    val memberId: Long,

    @field:NotNull(message = "질문 ID는 필수입니다.")
    @field:Min(value = 1, message = "유효하지 않은 질문 ID입니다.")
    val questionId: Long,

    @field:NotNull(message = "선택(YES/NO)은 필수입니다.")
    val choice: Choice,

    @field:NotNull(message = "베팅 금액은 필수입니다.")
    @field:Min(value = 100, message = "최소 베팅 금액은 100P입니다.")
    val amount: Long,

    val latencyMs: Int? = null
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
    val finalResult: String?,
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val yesPercentage: Double,
    val noPercentage: Double,
    val sourceUrl: String?,
    val disputeDeadline: String?,
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
