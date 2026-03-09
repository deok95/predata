package com.predata.backend.dto

import com.predata.backend.domain.SettlementReviewQueue
import com.predata.backend.domain.SettlementReviewReasonCode
import com.predata.backend.domain.SettlementReviewStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class SettlementReviewQueueResponse(
    val id: Long,
    val questionId: Long,
    val status: SettlementReviewStatus,
    val reasonCode: SettlementReviewReasonCode,
    val reasonDetail: String?,
    val retryCount: Int,
    val maxRetry: Int,
    val nextRetryAt: LocalDateTime?,
    val lastTriedAt: LocalDateTime?,
    val lastError: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(entry: SettlementReviewQueue) = SettlementReviewQueueResponse(
            id = entry.id!!,
            questionId = entry.questionId,
            status = entry.status,
            reasonCode = entry.reasonCode,
            reasonDetail = entry.reasonDetail,
            retryCount = entry.retryCount,
            maxRetry = entry.maxRetry,
            nextRetryAt = entry.nextRetryAt,
            lastTriedAt = entry.lastTriedAt,
            lastError = entry.lastError,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt,
        )
    }
}

data class ManualResolveRequest(
    @field:NotBlank(message = "finalResult (YES/NO) is required.")
    val finalResult: String,

    val sourceUrl: String? = null,
)
