package com.predata.backend.dto

import java.time.LocalDateTime

/**
 * On-chain betting data
 */
data class BetOnChainData(
    val questionId: Long,
    val userAddress: String?,
    val choice: Boolean, // true = YES, false = NO
    val amount: Long
)

/**
 * On-chain question data (for queries)
 */
data class QuestionOnChain(
    val questionId: Long,
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val settled: Boolean
)

/**
 * Blockchain status response
 */
data class BlockchainStatusResponse(
    val enabled: Boolean,
    val network: String,
    val totalQuestions: Int,
    val totalTransactions: Int
)
