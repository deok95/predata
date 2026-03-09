package com.predata.backend.domain.policy

data class VoteAvailability(
    val canVote: Boolean,
    val alreadyVoted: Boolean = false,
    val remainingVotes: Int = 0,
    val reason: VoteUnavailableReason? = null,
)

enum class VoteUnavailableReason {
    VOTING_CLOSED,
    ALREADY_VOTED,
    DAILY_LIMIT_EXCEEDED,
}

/**
 * Domain policy for vote limits and vote availability checks.
 */
object VotePolicy {
    private const val ADMIN_DAILY_LIMIT = 100

    fun resolveDailyLimit(role: String?, defaultDailyLimit: Int): Int {
        return if (role?.uppercase() == "ADMIN") ADMIN_DAILY_LIMIT else defaultDailyLimit
    }

    fun remainingDailyVotes(usedCount: Int, role: String?, defaultDailyLimit: Int): Int {
        val dailyLimit = resolveDailyLimit(role, defaultDailyLimit)
        return maxOf(0, dailyLimit - usedCount)
    }

    fun evaluate(
        isVotingOpen: Boolean,
        alreadyVoted: Boolean,
        usedCount: Int,
        role: String?,
        defaultDailyLimit: Int,
    ): VoteAvailability {
        if (!isVotingOpen) {
            return VoteAvailability(canVote = false, reason = VoteUnavailableReason.VOTING_CLOSED)
        }
        if (alreadyVoted) {
            return VoteAvailability(canVote = false, alreadyVoted = true, reason = VoteUnavailableReason.ALREADY_VOTED)
        }

        val remaining = remainingDailyVotes(usedCount, role, defaultDailyLimit)
        if (remaining <= 0) {
            return VoteAvailability(canVote = false, remainingVotes = 0, reason = VoteUnavailableReason.DAILY_LIMIT_EXCEEDED)
        }

        return VoteAvailability(canVote = true, remainingVotes = remaining)
    }
}
