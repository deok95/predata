package com.predata.backend.domain

enum class SettlementReviewStatus {
    PENDING_RETRY,  // 재시도 예정
    NEEDS_MANUAL,   // 수동처리 대기
    RESOLVED,       // 처리 완료
}
