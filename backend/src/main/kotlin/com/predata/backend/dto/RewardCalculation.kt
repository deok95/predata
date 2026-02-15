package com.predata.backend.dto

import java.math.BigDecimal

/**
 * 보상 계산 결과 DTO
 */
data class RewardCalculation(
    val memberId: Long,
    val level: Int,
    val weight: BigDecimal,
    val rewardAmount: BigDecimal,
    val questionId: Long
)

/**
 * 보상 계산 요약
 */
data class RewardCalculationSummary(
    val questionId: Long,
    val totalRewardPool: BigDecimal,
    val totalWeightSum: BigDecimal,
    val individualRewards: List<RewardCalculation>,
    val totalDistributed: BigDecimal,
    val truncatedAmount: BigDecimal, // 0.01 미만 절사된 총액
    val participantCount: Int
)
