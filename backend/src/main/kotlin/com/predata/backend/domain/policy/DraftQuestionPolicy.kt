package com.predata.backend.domain.policy

import com.predata.backend.domain.VoteWindowType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

data class DraftFeeSplit(
    val platformFeeShare: BigDecimal,
    val creatorFeeShare: BigDecimal,
    val voterFeeShare: BigDecimal,
)

object DraftQuestionPolicy {
    private val PLATFORM_FEE = BigDecimal("0.2000")
    private val DISTRIBUTABLE_FEE = BigDecimal("0.8000")

    fun requiredCredits(voteWindowType: VoteWindowType): Int = when (voteWindowType) {
        VoteWindowType.H3 -> 1
        VoteWindowType.H6 -> 1
        VoteWindowType.D1 -> 1
        VoteWindowType.D3 -> 3
    }

    fun votingDuration(voteWindowType: VoteWindowType): Duration = when (voteWindowType) {
        VoteWindowType.H3 -> VoteWindowType.H3.duration
        VoteWindowType.H6 -> VoteWindowType.H6.duration
        VoteWindowType.D1 -> VoteWindowType.D1.duration
        VoteWindowType.D3 -> VoteWindowType.D3.duration
    }

    fun maxBettingDuration(voteWindowType: VoteWindowType): Duration = when (voteWindowType) {
        VoteWindowType.H3 -> Duration.ofDays(1)
        VoteWindowType.H6 -> Duration.ofDays(1)
        VoteWindowType.D1 -> Duration.ofDays(3)
        VoteWindowType.D3 -> Duration.ofDays(7)
    }

    fun feeSplit(creatorSplitInPool: Int): DraftFeeSplit {
        val creatorPoolRatio = BigDecimal.valueOf(creatorSplitInPool.toLong())
            .divide(BigDecimal("100"), 4, RoundingMode.UNNECESSARY)
        val creatorFee = DISTRIBUTABLE_FEE.multiply(creatorPoolRatio).setScale(4, RoundingMode.UNNECESSARY)
        val voterFee = DISTRIBUTABLE_FEE.subtract(creatorFee).setScale(4, RoundingMode.UNNECESSARY)
        return DraftFeeSplit(
            platformFeeShare = PLATFORM_FEE,
            creatorFeeShare = creatorFee,
            voterFeeShare = voterFee,
        )
    }
}
