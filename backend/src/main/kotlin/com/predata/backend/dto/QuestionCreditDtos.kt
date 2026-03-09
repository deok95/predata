package com.predata.backend.dto

import com.predata.backend.domain.SettlementMode
import com.predata.backend.domain.VoteWindowType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

// ── Request ──────────────────────────────────────────────────────────────────

data class SubmitQuestionDraftRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 5, max = 200, message = "제목은 5자 이상 200자 이하여야 합니다.")
    val title: String,

    @field:NotBlank(message = "카테고리는 필수입니다.")
    val category: String,

    @field:Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    val description: String? = null,

    @field:NotNull(message = "투표 기간(voteWindowType)은 필수입니다.")
    val voteWindowType: VoteWindowType,

    @field:NotNull(message = "정산 방식(settlementMode)은 필수입니다.")
    val settlementMode: SettlementMode,

    /** OBJECTIVE_RULE 시 필수 */
    val resolutionRule: String? = null,

    /** OBJECTIVE_RULE 시 필수 */
    val resolutionSource: String? = null,

    @field:Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
    val thumbnailUrl: String? = null,

    val tags: List<@Size(min = 1, max = 30, message = "태그 길이는 1~30자여야 합니다.") String> = emptyList(),

    val sourceLinks: List<@Size(min = 1, max = 500, message = "소스 링크 길이는 1~500자여야 합니다.") String> = emptyList(),

    val boostEnabled: Boolean = false,

    /**
     * 80% 풀 안에서 생성자 비율(0~100, 10단위)
     * - platform은 항상 20% 고정
     */
    val creatorSplitInPool: Int = 50,
)

// ── Response ─────────────────────────────────────────────────────────────────

data class CreditStatusResponse(
    val yearlyBudget: Int,
    val usedCredits: Int,
    val availableCredits: Int,
    val requiredCredits: Int,
    val requiredCreditsByVoteWindow: Map<String, Int>,
    val resetAtUtc: String,
)

data class DraftOpenResponse(
    val draftId: String,
    val expiresAt: String,
    /** submit 시 X-Idempotency-Key 헤더로 전달 */
    val submitIdempotencyKey: String,
)

data class DraftSubmitResponse(
    val questionId: Long,
    val remainingCredits: Int,
    val usedCredits: Int,
    val votingEndAt: String,
)
