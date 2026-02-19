package com.predata.backend.dto

import java.math.BigDecimal

/**
 * Reward calculation result DTO
 */
data class RewardCalculation(
    val memberId: Long,
    val level: Int,
    val weight: BigDecimal,
    val rewardAmount: BigDecimal,
    val questionId: Long
)

/**
 * Reward calculation summary
 */
data class RewardCalculationSummary(
    val questionId: Long,
    val totalRewardPool: BigDecimal,
    val totalWeightSum: BigDecimal,
    val individualRewards: List<RewardCalculation>,
    val totalDistributed: BigDecimal,
    val truncatedAmount: BigDecimal, // Total amount truncated below 0.01
    val participantCount: Int
)
