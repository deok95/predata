package com.predata.backend.dto

import com.predata.backend.domain.Choice
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
