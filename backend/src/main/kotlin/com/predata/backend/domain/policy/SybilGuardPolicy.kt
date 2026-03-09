package com.predata.backend.domain.policy

import java.time.Duration
import java.time.LocalDateTime

object SybilGuardPolicy {
    fun accountAgeDays(createdAt: LocalDateTime, now: LocalDateTime): Long =
        Duration.between(createdAt, now).toDays()

    fun hasMinimumAccountAge(accountAgeDays: Long, minAccountAgeDays: Int): Boolean =
        accountAgeDays >= minAccountAgeDays

    fun hasMinimumVoteHistory(voteCount: Long, minVoteHistory: Int): Boolean =
        voteCount >= minVoteHistory.toLong()

    fun isEligibleForReward(accountAgeDays: Long, voteCount: Long, minAccountAgeDays: Int, minVoteHistory: Int): Boolean =
        hasMinimumAccountAge(accountAgeDays, minAccountAgeDays) && hasMinimumVoteHistory(voteCount, minVoteHistory)
}
