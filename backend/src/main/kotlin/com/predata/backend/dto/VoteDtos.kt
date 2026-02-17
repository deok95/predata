package com.predata.backend.dto

import com.predata.backend.domain.Choice
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Commit 요청 (1단계)
 * - commitHash: SHA-256(choice + salt) 해시값
 */
data class VoteCommitRequest(
    @field:NotNull(message = "질문 ID는 필수입니다.")
    val questionId: Long,

    @field:NotBlank(message = "커밋 해시는 필수입니다.")
    val commitHash: String
)

/**
 * Commit 응답
 */
data class VoteCommitResponse(
    val success: Boolean,
    val message: String,
    val voteCommitId: Long? = null,
    val remainingTickets: Int? = null
)

/**
 * Reveal 요청 (2단계)
 * - choice: 실제 선택 (YES or NO)
 * - salt: Commit 시 사용한 salt
 */
data class VoteRevealRequest(
    @field:NotNull(message = "질문 ID는 필수입니다.")
    val questionId: Long,

    @field:NotNull(message = "선택(YES/NO)은 필수입니다.")
    val choice: Choice,

    @field:NotBlank(message = "Salt는 필수입니다.")
    val salt: String
)

/**
 * Reveal 응답
 */
data class VoteRevealResponse(
    val success: Boolean,
    val message: String
)
