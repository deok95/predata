package com.predata.backend.service.amm

import com.predata.backend.dto.amm.SwapRequest
import com.predata.backend.dto.amm.SwapResponse
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

/**
 * SwapService의 Retry 래퍼
 *
 * @Retryable과 @Transactional을 분리하기 위한 Facade 패턴
 * - @Retryable이 @Transactional 바깥에 위치
 * - 재시도마다 새 트랜잭션이 열려서 최신 DB 상태를 읽음
 */
@Service
class RetryableSwapFacade(
    private val swapService: SwapService
) {

    /**
     * 스왑 실행 with Retry
     *
     * OptimisticLocking 실패 시 자동 재시도:
     * - 최대 3번 시도
     * - 50ms → 100ms → 200ms 백오프
     */
    @Retryable(
        retryFor = [OptimisticLockingFailureException::class, ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50, multiplier = 2.0)
    )
    fun executeSwapWithRetry(memberId: Long, request: SwapRequest): SwapResponse {
        return swapService.executeSwap(memberId, request)
    }
}
