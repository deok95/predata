package com.predata.backend.service.relay

/**
 * 온체인 릴레이 실행 결과
 *
 * - Success: 릴레이 성공, txHash 포함
 * - RetryableFailure: 일시적 오류, 백오프 후 재시도 가능
 * - FinalFailure: 복구 불가 오류, 즉시 FAILED_FINAL 전환
 */
sealed class RelayResult {

    data class Success(
        val txHash: String,
    ) : RelayResult()

    data class RetryableFailure(
        val reason: String,
        val errorMessage: String,
    ) : RelayResult()

    data class FinalFailure(
        val reason: String,
        val errorMessage: String,
    ) : RelayResult()
}
