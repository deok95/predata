package com.predata.backend.dto

import com.predata.backend.domain.Choice
import com.predata.backend.domain.VoteVisibility
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Commit request (Phase 1)
 * - commitHash: SHA-256(choice + salt) hash value
 */
data class VoteCommitRequest(
    @field:NotNull(message = "Question ID is required.")
    val questionId: Long,

    @field:NotBlank(message = "Commit hash is required.")
    val commitHash: String
)

/**
 * Commit response
 */
data class VoteCommitResponse(
    val success: Boolean,
    val message: String,
    val voteCommitId: Long? = null,
    val remainingTickets: Int? = null
)

/**
 * Reveal request (Phase 2)
 * - choice: Actual selection (YES or NO)
 * - salt: Salt used during commit
 */
data class VoteRevealRequest(
    @field:NotNull(message = "Question ID is required.")
    val questionId: Long,

    @field:NotNull(message = "Choice (YES/NO) is required.")
    val choice: Choice,

    @field:NotBlank(message = "Salt is required.")
    val salt: String
)

/**
 * Reveal response
 */
data class VoteRevealResponse(
    val success: Boolean,
    val message: String
)

/**
 * POST /api/votes 응답
 */
data class VoteResponse(
    val voteId: Long,
    val questionId: Long,
    val choice: Choice,
    val remainingDailyVotes: Int,
    val onChainStatus: String,
)

/**
 * 투표 가능 여부 상태 응답
 * - canVote=false 시 reason 필드로 사유 전달
 * - voteVisibility: OPEN(오픈 투표), HIDDEN_UNTIL_REVEAL(커밋-리빌 리빌 전), REVEALED(리빌 완료)
 */
data class VoteStatusResponse(
    val canVote: Boolean,
    val alreadyVoted: Boolean = false,
    val remainingDailyVotes: Int = 0,
    val reason: String? = null,
    val voteVisibility: VoteVisibility = VoteVisibility.OPEN,
)
