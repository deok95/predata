package com.predata.backend.service.relay

import com.predata.backend.domain.OnChainVoteRelay

/**
 * 온체인 투표 릴레이 인터페이스
 *
 * 구현체 교체 가능 구조:
 * - MockOnChainRelayService  (relay.mode=mock, 베타 기본)
 * - PolygonOnChainRelayService (relay.mode=polygon, 실제 체인)
 */
interface OnChainRelayService {

    /**
     * 릴레이 항목을 블록체인에 제출한다.
     *
     * @return RelayResult.Success          – 온체인 기록 성공 (txHash 포함)
     *         RelayResult.RetryableFailure – 일시 오류, 백오프 후 재시도 가능
     *         RelayResult.FinalFailure     – 복구 불가 오류
     */
    fun relay(relay: OnChainVoteRelay): RelayResult
}
