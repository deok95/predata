package com.predata.backend.dto

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// Vote request
data class VoteRequest(
    @field:NotNull(message = "Question ID is required.")
    @field:Min(value = 1, message = "Invalid question ID.")
    val questionId: Long,

    @field:NotNull(message = "Choice (YES/NO) is required.")
    val choice: Choice,

    val latencyMs: Int? = null
)

// Bet request
data class BetRequest(
    @field:NotNull(message = "Question ID is required.")
    @field:Min(value = 1, message = "Invalid question ID.")
    val questionId: Long,

    @field:NotNull(message = "Choice (YES/NO) is required.")
    val choice: Choice,

    @field:NotNull(message = "Bet amount is required.")
    @field:Min(value = 1, message = "Minimum bet amount is 1P.")
    @field:Max(value = 100, message = "Maximum bet amount is 100P.")
    val amount: Long,

    val latencyMs: Int? = null
)

// Bet sell request
data class SellBetRequest(
    @field:NotNull(message = "Bet ID is required.")
    @field:Min(value = 1, message = "Invalid bet ID.")
    val betId: Long
)

// Bet sell response
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

// Activity response
data class ActivityResponse(
    val success: Boolean,
    val message: String? = null,
    val activityId: Long? = null,
    val remainingTickets: Int? = null
)

// Question response
data class QuestionResponse(
    val id: Long,
    val title: String,
    val category: String?,
    val status: String,
    val type: String,
    val executionModel: String,
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
    val createdAt: String,
    val viewCount: Long = 0,
    val matchId: Long? = null
)

// Ticket status response
data class TicketStatusResponse(
    val remainingCount: Int,
    val resetDate: String
)

// === Authentication request DTOs ===

// Step 1: Send verification code to email
data class SendCodeRequest(
    val email: String
)

// Step 2: Verify code only (does not create member)
data class VerifyCodeRequest(
    val email: String,
    val code: String
)

// Step 3: Set password and create member
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
data class CompleteSignupRequest(
    val email: String,
    val code: String,
    val password: String,
    val passwordConfirm: String,

    // Additional information (optional - nullable with defaults)
    val countryCode: String = "KR",
    val gender: String? = null,
    val birthDate: String? = null, // ISO 8601 format (YYYY-MM-DD)
    val jobCategory: String? = null,
    val ageGroup: Int? = null
)

// Login request
data class LoginRequest(
    val email: String,
    val password: String
)

// === Admin question creation DTO ===

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
data class AdminCreateQuestionRequest(
    @field:NotNull(message = "Question title is required.")
    val title: String,

    @field:NotNull(message = "Question type is required.")
    val type: com.predata.backend.domain.QuestionType,

    @field:NotNull(message = "Market type is required.")
    val marketType: com.predata.backend.domain.MarketType = com.predata.backend.domain.MarketType.VERIFIABLE,

    @field:NotBlank(message = "Resolution rule is required.")
    val resolutionRule: String,

    val resolutionSource: String? = null,

    val resolveAt: String? = null, // ISO 8601 format

    val disputeUntil: String? = null, // ISO 8601 format

    // Recommended true for OPINION markets (settle betting based on vote results)
    val voteResultSettlement: Boolean? = null,

    val category: String? = null,

    @field:Min(value = 60, message = "Voting period must be at least 60 seconds.")
    val votingDuration: Long = 3600, // Default 1 hour (in seconds)

    @field:Min(value = 60, message = "Betting period must be at least 60 seconds.")
    val bettingDuration: Long = 3600, // Default 1 hour (in seconds)

    // AMM execution model configuration
    val executionModel: com.predata.backend.domain.ExecutionModel = com.predata.backend.domain.ExecutionModel.AMM_FPMM,

    // AMM seed configuration (used when executionModel is AMM_FPMM)
    val seedUsdc: java.math.BigDecimal = java.math.BigDecimal("1000"),

    val feeRate: java.math.BigDecimal = java.math.BigDecimal("0.01") // Default 1%
)

// Admin result input request
data class SetQuestionResultRequest(
    @field:NotNull(message = "Result (YES/NO) is required.")
    val result: String // "YES" or "NO"
)

// Admin manual settlement request
data class ManualSettleRequest(
    @field:NotNull(message = "Result (YES/NO/DRAW) is required.")
    val result: String // "YES", "NO", "DRAW"
)

// ============================================================
// Google OAuth DTOs
// ============================================================

// Google OAuth login request
data class GoogleAuthRequest(
    @field:NotBlank(message = "Google token is required")
    val googleToken: String,         // ID Token received from Google
    val countryCode: String? = null, // Additional information (optional)
    val jobCategory: String? = null,
    val ageGroup: Int? = null
)

// Google OAuth login response
data class GoogleAuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,           // JWT token
    val memberId: Long? = null,
    val needsAdditionalInfo: Boolean = false  // Whether additional information is needed
)

// Google OAuth registration completion request (additional information)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
data class CompleteGoogleRegistrationRequest(
    @field:NotBlank(message = "Google ID is required")
    val googleId: String,
    @field:NotBlank(message = "Email is required")
    val email: String,

    // Additional information (optional - nullable with defaults)
    val countryCode: String = "KR",
    val gender: String? = null,
    val birthDate: String? = null, // ISO 8601 format (YYYY-MM-DD)
    val jobCategory: String? = null,
    val ageGroup: Int? = null
)
