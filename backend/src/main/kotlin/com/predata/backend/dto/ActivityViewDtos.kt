package com.predata.backend.dto

import com.predata.backend.domain.Activity

/**
 * 활동 조회 응답 DTO들 및 변환기
 * - 엔드포인트별 기존 응답 필드를 유지하기 위해 DTO를 분리
 */
data class MemberActivityView(
    val id: Long?,
    val questionId: Long,
    val activityType: String,
    val choice: String,
    val amount: Long,
    val createdAt: String
)

data class QuestionActivityView(
    val id: Long?,
    val memberId: Long,
    val questionId: Long,
    val activityType: String,
    val choice: String,
    val amount: Long,
    val latencyMs: Int?,
    val parentBetId: Long?,
    val createdAt: String
)

data class MemberQuestionActivityView(
    val id: Long?,
    val questionId: Long,
    val activityType: String,
    val choice: String,
    val amount: Long,
    val latencyMs: Int?,
    val parentBetId: Long?,
    val createdAt: String
)

object ActivityViewAssembler {
    fun toMemberActivityView(activity: Activity): MemberActivityView {
        return MemberActivityView(
            id = activity.id,
            questionId = activity.questionId,
            activityType = activity.activityType.name,
            choice = activity.choice.name,
            amount = activity.amount,
            createdAt = activity.createdAt.toString()
        )
    }

    fun toQuestionActivityView(activity: Activity): QuestionActivityView {
        return QuestionActivityView(
            id = activity.id,
            memberId = activity.memberId,
            questionId = activity.questionId,
            activityType = activity.activityType.name,
            choice = activity.choice.name,
            amount = activity.amount,
            latencyMs = activity.latencyMs,
            parentBetId = activity.parentBetId,
            createdAt = activity.createdAt.toString()
        )
    }

    fun toMemberQuestionActivityView(activity: Activity): MemberQuestionActivityView {
        return MemberQuestionActivityView(
            id = activity.id,
            questionId = activity.questionId,
            activityType = activity.activityType.name,
            choice = activity.choice.name,
            amount = activity.amount,
            latencyMs = activity.latencyMs,
            parentBetId = activity.parentBetId,
            createdAt = activity.createdAt.toString()
        )
    }
}
