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
    RISK_LIMIT_EXCEEDED
}
