package com.predata.backend.sports.scheduler

import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.event.MatchFinishedEvent
import com.predata.backend.sports.event.MatchGoalEvent
import com.predata.backend.sports.provider.football.FootballDataProvider
import com.predata.backend.sports.repository.LeagueRepository
import com.predata.backend.sports.repository.MatchRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class MatchSyncScheduler(
    private val footballDataProvider: FootballDataProvider,
    private val matchRepository: MatchRepository,
    private val leagueRepository: LeagueRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(MatchSyncScheduler::class.java)

    /**
     * 매일 06:00 → PL 경기 동기화 (신규 insert, 기존 update)
     */
    @Scheduled(cron = "\${sports.scheduler.sync-cron}")
    @Transactional
    fun syncUpcomingMatches() {
        logger.info("[MatchSync] 매일 동기화 시작")

        val league = leagueRepository.findByExternalLeagueIdAndProvider(
            "PL", FootballDataProvider.PROVIDER_NAME
        )
        if (league == null) {
            logger.warn("[MatchSync] PL 리그를 찾을 수 없습니다. (provider=${FootballDataProvider.PROVIDER_NAME})")
            return
        }

        val fetched = footballDataProvider.fetchUpcomingMatches(league)
        logger.info("[MatchSync] API에서 가져온 경기 수: ${fetched.size}")

        var inserted = 0
        var updated = 0

        for (apiMatch in fetched) {
            val externalId = apiMatch.externalMatchId ?: continue
            val existing = matchRepository.findByExternalMatchIdAndProvider(
                externalId, FootballDataProvider.PROVIDER_NAME
            )

            if (existing != null) {
                existing.homeScore = apiMatch.homeScore
                existing.awayScore = apiMatch.awayScore
                existing.matchStatus = apiMatch.matchStatus
                matchRepository.save(existing)
                updated++
            } else {
                matchRepository.save(apiMatch)
                inserted++
            }
        }

        logger.info("[MatchSync] 동기화 완료 - 신규: ${inserted}건, 업데이트: ${updated}건")
    }

    /**
     * LIVE 경기 존재 시 1분 간격 폴링
     * LIVE 경기 없으면 폴링 안 함
     */
    @Scheduled(fixedDelayString = "\${sports.scheduler.live-poll-interval-ms}")
    @Transactional
    fun pollLiveMatches() {
        val now = LocalDateTime.now()
        val candidates = matchRepository.findPollCandidates(now)

        if (candidates.isEmpty()) {
            logger.debug("[MatchSync] LIVE 폴링 - 대상 경기 없음, skip")
            return
        }

        logger.info("[MatchSync] LIVE 폴링 시작 - 대상 경기: ${candidates.size}건")

        for (match in candidates) {
            try {
                val externalId = match.externalMatchId ?: continue
                val apiMatch = footballDataProvider.fetchMatchResult(externalId) ?: continue

                val oldHomeScore = match.homeScore ?: 0
                val oldAwayScore = match.awayScore ?: 0
                val oldStatus = match.matchStatus

                val newHomeScore = apiMatch.homeScore ?: 0
                val newAwayScore = apiMatch.awayScore ?: 0
                val newStatus = apiMatch.matchStatus

                // DB 업데이트
                match.homeScore = apiMatch.homeScore
                match.awayScore = apiMatch.awayScore
                match.matchStatus = newStatus
                matchRepository.save(match)

                // 골 감지 → MatchGoalEvent 발행
                if (newHomeScore != oldHomeScore || newAwayScore != oldAwayScore) {
                    logger.info(
                        "[MatchSync] 골! {} {}-{} {} (이전: {}-{})",
                        match.homeTeam, newHomeScore, newAwayScore, match.awayTeam,
                        oldHomeScore, oldAwayScore
                    )
                    eventPublisher.publishEvent(
                        MatchGoalEvent(
                            source = this,
                            matchId = match.id!!,
                            homeScore = newHomeScore,
                            awayScore = newAwayScore,
                            minute = null
                        )
                    )
                }

                // LIVE → FINISHED 전환 감지 → MatchFinishedEvent 발행
                if (oldStatus != MatchStatus.FINISHED && newStatus == MatchStatus.FINISHED) {
                    logger.info(
                        "[MatchSync] 경기 종료: {} {}-{} {}",
                        match.homeTeam, newHomeScore, newAwayScore, match.awayTeam
                    )
                    eventPublisher.publishEvent(
                        MatchFinishedEvent(
                            source = this,
                            matchId = match.id!!,
                            homeScore = newHomeScore,
                            awayScore = newAwayScore
                        )
                    )
                }

                // API rate limit 방지
                Thread.sleep(400)

            } catch (e: Exception) {
                logger.error("[MatchSync] 폴링 실패: {} vs {} - {}", match.homeTeam, match.awayTeam, e.message)
            }
        }
    }
}
