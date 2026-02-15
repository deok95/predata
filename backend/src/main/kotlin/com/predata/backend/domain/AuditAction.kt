package com.predata.backend.domain

/**
 * 감사 로그 액션 타입
 */
enum class AuditAction {
    /** 주문 생성 */
    ORDER_CREATE,

    /** 주문 취소 */
    ORDER_CANCEL,

    /** 포지션 업데이트 */
    POSITION_UPDATE,

    /** 정산 시작 */
    SETTLE,

    /** 정산 취소 */
    CANCEL,

    /** 권한 거부 (403) */
    PERMISSION_DENIED,

    /** 출금 */
    WITHDRAWAL,

    /** 입금 */
    DEPOSIT,

    /** 리스크 한도 초과 */
    RISK_LIMIT_EXCEEDED,

    // === 투표 시스템 액션 ===
    /** 투표 커밋 */
    VOTE_COMMIT,

    /** 투표 공개 */
    VOTE_REVEAL,

    /** 보상 계산 */
    REWARD_CALCULATED,

    /** 보상 분배 */
    REWARD_DISTRIBUTED,

    /** 보상 분배 실패 */
    REWARD_FAILED,

    /** 수수료 수집 */
    FEE_COLLECTED
}
