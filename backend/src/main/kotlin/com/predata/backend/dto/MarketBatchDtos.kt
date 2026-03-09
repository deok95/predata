package com.predata.backend.dto

import com.predata.backend.domain.market.MarketOpenBatch
import com.predata.backend.domain.market.QuestionMarketCandidate

// ─── 배치 목록 응답 ────────────────────────────────────────────────────────────

data class MarketBatchSummaryResponse(
    val id: Long,
    val cutoffSlotUtc: String,
    val status: String,
    val startedAt: String,
    val finishedAt: String?,
    val totalCandidates: Int,
    val selectedCount: Int,
    val openedCount: Int,
    val failedCount: Int,
    val errorSummary: String?,
)

fun MarketOpenBatch.toSummaryResponse() = MarketBatchSummaryResponse(
    id = id!!,
    cutoffSlotUtc = cutoffSlotUtc.toString(),
    status = status.name,
    startedAt = startedAt.toString(),
    finishedAt = finishedAt?.toString(),
    totalCandidates = totalCandidates,
    selectedCount = selectedCount,
    openedCount = openedCount,
    failedCount = failedCount,
    errorSummary = errorSummary,
)

// ─── 후보 목록 응답 ────────────────────────────────────────────────────────────

data class MarketCandidateResponse(
    val id: Long,
    val batchId: Long,
    val questionId: Long,
    val category: String,
    val voteCount: Long,
    val rankInCategory: Int,
    val selectionStatus: String,
    val selectionReason: String?,
    val canonicalQuestionId: Long?,
    val openStatus: String?,
    val openError: String?,
    val createdAt: String,
    val updatedAt: String,
)

fun QuestionMarketCandidate.toResponse() = MarketCandidateResponse(
    id = id!!,
    batchId = batchId,
    questionId = questionId,
    category = category,
    voteCount = voteCount,
    rankInCategory = rankInCategory,
    selectionStatus = selectionStatus.name,
    selectionReason = selectionReason?.name,
    canonicalQuestionId = canonicalQuestionId,
    openStatus = openStatus?.name,
    openError = openError,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

// ─── 집계 요약 응답 ────────────────────────────────────────────────────────────

data class MarketBatchSummaryDetailResponse(
    val batchId: Long,
    val status: String,
    val totalCandidates: Int,
    val selectedCount: Int,
    val openedCount: Int,
    val failedCount: Int,
    val successRate: Double,
)

// ─── 재시도 요청/응답 ──────────────────────────────────────────────────────────

data class RetryOpenResponse(
    val batchId: Long,
    val status: String,
    val openedCount: Int,
    val failedCount: Int,
)
