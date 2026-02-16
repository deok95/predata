package com.predata.backend.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 투표 서킷브레이커
 * - 1분 내 투표 실패율 50% 이상이면 자동 전체 중지
 * - 자동 중지 후 5분 뒤 반자동 재개 (half-open → 성공 시 close)
 */
@Service
class VotingCircuitBreaker(
    private val pauseService: PauseService
) {
    private val logger = LoggerFactory.getLogger(VotingCircuitBreaker::class.java)

    companion object {
        const val WINDOW_SECONDS = 60L  // 1분 윈도우
        const val FAILURE_THRESHOLD = 0.5  // 실패율 50%
        const val COOLDOWN_SECONDS = 300L  // 5분 쿨다운
    }

    // 상태
    private enum class State {
        CLOSED,      // 정상 동작
        OPEN,        // 서킷 오픈 (전체 중지)
        HALF_OPEN    // 반자동 재개 (테스트 중)
    }

    @Volatile
    private var state = State.CLOSED

    @Volatile
    private var lastOpenTime: LocalDateTime? = null

    // 기록 (timestamp, isSuccess)
    private val records = ConcurrentLinkedQueue<Pair<LocalDateTime, Boolean>>()

    /**
     * 성공 기록
     */
    fun recordSuccess() {
        records.add(Pair(LocalDateTime.now(ZoneOffset.UTC), true))
        cleanOldRecords()

        // HALF_OPEN 상태에서 성공하면 CLOSED로 전환
        if (state == State.HALF_OPEN) {
            state = State.CLOSED
            pauseService.resumeAll()
            logger.info("Circuit breaker closed after successful operation in HALF_OPEN state")
        }
    }

    /**
     * 실패 기록
     */
    fun recordFailure() {
        records.add(Pair(LocalDateTime.now(ZoneOffset.UTC), false))
        cleanOldRecords()
        checkFailureRate()
    }

    /**
     * 서킷브레이커가 열려있는지 확인 (중지 상태)
     */
    fun isOpen(): Boolean {
        // OPEN 상태에서 쿨다운 시간이 지났으면 HALF_OPEN으로 전환
        if (state == State.OPEN) {
            val openTime = lastOpenTime
            if (openTime != null) {
                val elapsed = java.time.Duration.between(openTime, LocalDateTime.now(ZoneOffset.UTC)).seconds
                if (elapsed >= COOLDOWN_SECONDS) {
                    state = State.HALF_OPEN
                    logger.info("Circuit breaker entering HALF_OPEN state after cooldown")
                }
            }
        }

        return state == State.OPEN
    }

    /**
     * 현재 상태 조회
     */
    fun getState(): String = state.name

    /**
     * 현재 통계 조회
     */
    fun getStats(): Map<String, Any> {
        cleanOldRecords()
        val recentRecords = records.toList()
        val total = recentRecords.size
        val failures = recentRecords.count { !it.second }
        val failureRate = if (total > 0) failures.toDouble() / total else 0.0

        return mapOf(
            "state" to state.name,
            "totalRecords" to total,
            "failures" to failures,
            "failureRate" to failureRate,
            "lastOpenTime" to (lastOpenTime?.toString() ?: "N/A")
        )
    }

    /**
     * 1분 이전 기록 제거
     */
    private fun cleanOldRecords() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val cutoff = now.minusSeconds(WINDOW_SECONDS)

        while (true) {
            val record = records.peek() ?: break
            if (record.first.isBefore(cutoff)) {
                records.poll()
            } else {
                break
            }
        }
    }

    /**
     * 실패율 체크 및 서킷브레이커 오픈
     */
    private fun checkFailureRate() {
        val recentRecords = records.toList()
        val total = recentRecords.size
        val failures = recentRecords.count { !it.second }

        if (total == 0) return

        val failureRate = failures.toDouble() / total

        // 실패율이 50% 이상이고, CLOSED 상태이면 OPEN으로 전환
        if (failureRate >= FAILURE_THRESHOLD && state == State.CLOSED) {
            state = State.OPEN
            lastOpenTime = LocalDateTime.now(ZoneOffset.UTC)
            pauseService.pauseAll()
            logger.error("Circuit breaker OPENED: failureRate=$failureRate, failures=$failures, total=$total")
        }
    }

    /**
     * 수동 리셋 (관리자용)
     */
    fun reset() {
        state = State.CLOSED
        lastOpenTime = null
        records.clear()
        pauseService.resumeAll()
        logger.info("Circuit breaker manually reset")
    }
}
