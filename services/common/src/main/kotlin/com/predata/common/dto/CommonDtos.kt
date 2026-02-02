package com.predata.common.dto

import java.math.BigDecimal
import java.time.LocalDateTime

// API Response Wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null
)

// Member DTO
data class MemberDto(
    val id: Long,
    val email: String,
    val walletAddress: String?,
    val countryCode: String,
    val jobCategory: String?,
    val ageGroup: String?,
    val tier: String,
    val tierWeight: BigDecimal,
    val accuracyScore: Int,
    val pointBalance: Long,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val createdAt: LocalDateTime
)

// Question DTO
data class QuestionDto(
    val id: Long,
    val title: String,
    val category: String?,
    val status: String, // OPEN, CLOSED, SETTLED
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val finalResult: String?, // YES, NO
    val expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime
)

// Activity DTO
data class ActivityDto(
    val id: Long,
    val memberId: Long,
    val questionId: Long,
    val activityType: String, // VOTE, BET
    val choice: String, // YES, NO
    val amount: Long,
    val latencyMs: Long?,
    val createdAt: LocalDateTime
)

// Update Pool Request
data class UpdatePoolRequest(
    val choice: String, // YES or NO
    val amount: Long
)

// Deduct/Add Points Request
data class PointsRequest(
    val amount: Long
)

// Final Result Request
data class FinalResultRequest(
    val finalResult: String, // YES or NO
    val status: String = "SETTLED"
)
