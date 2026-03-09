package com.predata.backend.domain

enum class SettlementMode {
    /** 투표 다수결로 정산 — OPINION 시장 */
    VOTE_RESULT,

    /** 외부 사실 기반 정산 — VERIFIABLE 시장, source/rule 필수 */
    OBJECTIVE_RULE,
}
