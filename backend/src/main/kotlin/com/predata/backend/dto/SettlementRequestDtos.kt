package com.predata.backend.dto

/**
 * Settlement request DTO
 */
data class SettleQuestionRequest(
    val finalResult: String, // "YES" or "NO"
    val sourceUrl: String? = null // Settlement evidence link
)

/**
 * Settlement finalization request DTO
 */
data class FinalizeSettlementRequest(
    val force: Boolean = false // If true, immediately finalize ignoring dispute period
)
