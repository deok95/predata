package com.predata.backend.service.relay

import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.OnChainVoteRelay
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.OnChainVoteRelayRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 릴레이 운영 관리 서비스
 *
 * 운영자가 FAILED_FINAL 항목을 수동으로 PENDING으로 되돌려 재처리할 수 있다.
 */
@Service
class RelayAdminService(
    private val onChainVoteRelayRepository: OnChainVoteRelayRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * FAILED_FINAL → PENDING 강제 전환 (retry_count 리셋)
     *
     * 운영자가 외부 체인 오류 해소 후 수동 재처리 시 사용.
     */
    @Transactional
    fun forceRetry(relayId: Long): OnChainVoteRelay {
        val relay = onChainVoteRelayRepository.findById(relayId).orElseThrow {
            NotFoundException()
        }

        if (relay.status != OnChainRelayStatus.FAILED_FINAL) {
            throw ConflictException("Only FAILED_FINAL relays can be force-retried.")
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)
        relay.status = OnChainRelayStatus.PENDING
        relay.retryCount = 0
        relay.nextRetryAt = null
        relay.errorMessage = null
        relay.updatedAt = now

        logger.info("Force retry: relayId={} voteId={}", relayId, relay.voteId)
        return onChainVoteRelayRepository.save(relay)
    }
}
