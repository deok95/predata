package com.predata.backend.domain

enum class CreditLedgerReason {
    QUESTION_CREATED,       // 질문 등록으로 차감
    LOW_VOTE_CANCELLED,     // minVotes 미달로 CANCELLED → 환불
    ADMIN_GRANT,            // 관리자 지급
    ADMIN_DEDUCT,           // 관리자 차감
}
