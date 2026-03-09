package com.predata.backend.domain.policy

import java.math.BigDecimal
import java.math.RoundingMode

data class FeeDistribution(
    val platformAmount: BigDecimal,
    val creatorAmount: BigDecimal,
    val voterAmount: BigDecimal,
    val reserveAmount: BigDecimal,
)

object FeeDistributionPolicy {
    fun distribute(
        totalFee: BigDecimal,
        platformRatio: BigDecimal,
        creatorRatio: BigDecimal,
        voterRatio: BigDecimal,
        scale: Int = 6,
    ): FeeDistribution {
        val ratioSum = platformRatio.add(creatorRatio).add(voterRatio)
        require(ratioSum.compareTo(BigDecimal.ONE) == 0) {
            "Invalid fee share sum. sum=$ratioSum"
        }

        val platformAmount = totalFee.multiply(platformRatio).setScale(scale, RoundingMode.HALF_UP)
        val creatorAmount = totalFee.multiply(creatorRatio).setScale(scale, RoundingMode.HALF_UP)
        val voterAmount = totalFee.multiply(voterRatio).setScale(scale, RoundingMode.HALF_UP)
        val reserveAmount = totalFee
            .subtract(platformAmount)
            .subtract(creatorAmount)
            .subtract(voterAmount)
            .setScale(scale, RoundingMode.HALF_UP)

        return FeeDistribution(
            platformAmount = platformAmount,
            creatorAmount = creatorAmount,
            voterAmount = voterAmount,
            reserveAmount = reserveAmount,
        )
    }
}
