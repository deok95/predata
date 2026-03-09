package com.predata.backend.service.relay

import com.predata.backend.domain.OnChainVoteRelay
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Polygon 온체인 릴레이 구현체 (실제 체인 연동)
 *
 * relay.mode=polygon 시 활성화. MockOnChainRelayService 대신 주입된다.
 *
 * 구현 예정:
 * 1. Web3j / ethers4k로 Polygon RPC 호출
 * 2. 서명된 트랜잭션 제출 (sender-private-key)
 * 3. 예외 분류:
 *    - 네트워크 타임아웃, RPC 일시 오류 → RetryableFailure
 *    - 잘못된 데이터, 계약 거부 → FinalFailure
 * 4. txHash = 온체인 응답 tx.hash
 *
 * 주의: relay() 호출은 DB 트랜잭션 외부에서 수행해야 함.
 *       현재 스케줄러는 단일 트랜잭션 내에서 호출하므로,
 *       실제 구현 시 OnChainVoteRelayScheduler 에서 claimBatch / applyResult 를
 *       별도 트랜잭션으로 분리해야 한다.
 */
@Service
@ConditionalOnProperty(name = ["app.relay.mode"], havingValue = "polygon")
class PolygonOnChainRelayService : OnChainRelayService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun relay(relay: OnChainVoteRelay): RelayResult {
        logger.warn("Polygon relay not implemented: voteId={}", relay.voteId)
        return RelayResult.RetryableFailure(
            reason = "NOT_IMPLEMENTED",
            errorMessage = "Polygon relay implementation is pending.",
        )
    }
}
