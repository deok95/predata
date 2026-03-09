package com.predata.backend.domain

enum class SettlementReviewReasonCode {
    SOURCE_UNAVAILABLE,    // 어댑터가 데이터 소스를 아직 가져오지 못함
    NOT_FINISHED,          // 경기/이벤트 미종료
    RULE_PARSE_FAILED,     // resolutionRule 파싱 실패
    CONFIDENCE_LOW,        // confidence < 임계값
    VOTE_REVEAL_PENDING,   // VOTE_RESULT 질문인데 reveal 미완료
    EXCEPTION,             // 어댑터/서비스 예외 발생
}
