package com.predata.backend.service

import com.predata.backend.repository.DailyVoteUsageRepository
import com.predata.backend.repository.OnChainVoteRelayRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 투표 데이터 보관 배치 (VOTE-026)
 *
 * 정책:
 *   - daily_vote_usage:    90일 초과 레코드 삭제
 *   - onchain_vote_relays: 180일 초과 CONFIRMED/FAILED_FINAL 레코드 삭제
 *                          (PENDING/SUBMITTED/FAILED 는 미완료 상태라 제외)
 *
 * 주기: 매일 새벽 3시 UTC (트래픽 최저 구간)
 */
@Component
class VoteDataRetentionScheduler(
    private val dailyVoteUsageRepository: DailyVoteUsageRepository,
    private val onChainVoteRelayRepository: OnChainVoteRelayRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    fun purge() {
        purgeOldDailyUsage()
        purgeOldRelays()
    }

    private fun purgeOldDailyUsage() {
        val cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(DAILY_USAGE_RETENTION_DAYS)
        val deleted = dailyVoteUsageRepository.deleteOlderThan(cutoff)
        logger.info("Retention: deleted {} daily_vote_usage rows (cutoff={})", deleted, cutoff)
    }

    private fun purgeOldRelays() {
        val cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(RELAY_RETENTION_DAYS)
        val deleted = onChainVoteRelayRepository.deleteTerminalOlderThan(cutoff)
        logger.info("Retention: deleted {} terminal onchain_vote_relays rows (cutoff={})", deleted, cutoff)
    }

    companion object {
        private const val DAILY_USAGE_RETENTION_DAYS = 90L
        private const val RELAY_RETENTION_DAYS = 180L
    }
}
