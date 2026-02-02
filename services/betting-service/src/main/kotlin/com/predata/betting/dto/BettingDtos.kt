package com.predata.betting.dto

import com.predata.betting.domain.Activity

// === Request DTOs ===

data class VoteRequest(
    val memberId: Long,
    val questionId: Long,
    val choice: String, // "YES" or "NO"
    val latencyMs: Int? = null
)

data class BetRequest(
    val memberId: Long,
    val questionId: Long,
    val choice: String, // "YES" or "NO"
    val amount: Long,
    val latencyMs: Int? = null
)

// === Response DTOs ===

data class ActivityResponse(
    val success: Boolean,
    val message: String,
    val activityId: Long? = null
)

data class ActivityDetailResponse(
    val activityId: Long,
    val memberId: Long,
    val questionId: Long,
    val activityType: String,
    val choice: String,
    val amount: Long,
    val createdAt: String
) {
    companion object {
        fun from(activity: Activity) = ActivityDetailResponse(
            activityId = activity.id ?: 0,
            memberId = activity.memberId,
            questionId = activity.questionId,
            activityType = activity.activityType.name,
            choice = activity.choice.name,
            amount = activity.amount,
            createdAt = activity.createdAt.toString()
        )
    }
}
