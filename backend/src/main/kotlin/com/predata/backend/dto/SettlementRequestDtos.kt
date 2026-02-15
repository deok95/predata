package com.predata.backend.dto

/**
 * 정산 요청 DTO
 */
data class SettleQuestionRequest(
    val finalResult: String, // "YES" or "NO"
    val sourceUrl: String? = null // 정산 근거 링크
)

/**
 * 정산 확정 요청 DTO
 */
data class FinalizeSettlementRequest(
    val force: Boolean = false // true면 이의 제기 기간 무시하고 즉시 확정
)
