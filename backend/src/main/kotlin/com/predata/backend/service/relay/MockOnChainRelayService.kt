package com.predata.backend.service.relay

import com.predata.backend.domain.OnChainVoteRelay
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 목 온체인 릴레이 구현체 (베타 기본 경로)
 *
 * relay.mode=mock 또는 미설정 시 활성화.
 * 항상 CONFIRMED 성공으로 응답하며 txHash=mock_<uuid> 를 반환한다.
 */
@Service
@ConditionalOnProperty(name = ["app.relay.mode"], havingValue = "mock", matchIfMissing = true)
class MockOnChainRelayService : OnChainRelayService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun relay(relay: OnChainVoteRelay): RelayResult {
        val txHash = "mock_${UUID.randomUUID()}"
        logger.debug("Mock relay success: voteId={} txHash={}", relay.voteId, txHash)
        return RelayResult.Success(txHash)
    }
}
