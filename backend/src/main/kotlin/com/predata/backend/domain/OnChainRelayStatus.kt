package com.predata.backend.domain

/**
 * 온체인 투표 릴레이 상태 FSM
 *
 * PENDING → SUBMITTED → CONFIRMED
 *                     → FAILED        (retryCount < maxRetry, 백오프 후 PENDING 재진입)
 *                     → FAILED_FINAL  (retryCount >= maxRetry 또는 복구 불가 오류, 터미널)
 */
enum class OnChainRelayStatus {
    PENDING,
    SUBMITTED,
    CONFIRMED,
    FAILED,
    FAILED_FINAL,
}
