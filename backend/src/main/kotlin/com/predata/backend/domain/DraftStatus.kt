package com.predata.backend.domain

enum class DraftStatus {
    OPEN,       // 작성 가능
    CONSUMED,   // 제출 완료
    EXPIRED,    // 30분 만료
    CANCELLED,  // 사용자 취소
}
