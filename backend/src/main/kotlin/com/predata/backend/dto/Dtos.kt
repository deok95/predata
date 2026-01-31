package com.predata.backend.dto

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice

// 투표 요청
data class VoteRequest(
    val memberId: Long,
    val questionId: Long,
    val choice: Choice,
    val latencyMs: Int? = null
)

// 베팅 요청
data class BetRequest(
    val memberId: Long,
    val questionId: Long,
    val choice: Choice,
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
    val expiredAt: String,
    val createdAt: String
)

// 티켓 현황 응답
data class TicketStatusResponse(
    val remainingCount: Int,
    val resetDate: String
)
