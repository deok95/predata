package com.predata.backend.domain

/**
 * 투표 결과 공개 여부
 *
 * - OPEN: 실시간 yes/no 집계 공개 (OBJECTIVE_RULE 질문)
 * - HIDDEN_UNTIL_REVEAL: reveal 마감 전 yes/no 비공개, 참여 수만 공개 (VOTE_RESULT 질문 + reveal 미완료)
 * - REVEALED: reveal 완료 후 집계 공개 (VOTE_RESULT 질문 + VOTING_REVEAL_CLOSED 이후)
 */
enum class VoteVisibility {
    OPEN,
    HIDDEN_UNTIL_REVEAL,
    REVEALED,
}
