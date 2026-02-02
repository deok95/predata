package com.predata.blockchain.dto

import java.time.LocalDateTime

/**
 * 온체인 베팅 데이터
 */
data class BetOnChainData(
    val questionId: Long,
    val userAddress: String?,
    val choice: Boolean, // true = YES, false = NO
    val amount: Long
)

/**
 * 온체인 질문 데이터 (조회용)
 */
data class QuestionOnChain(
    val questionId: Long,
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val settled: Boolean
)

/**
 * 블록체인 상태 응답
 */
data class BlockchainStatusResponse(
    val enabled: Boolean,
    val network: String,
    val totalQuestions: Int,
    val totalTransactions: Int
)
