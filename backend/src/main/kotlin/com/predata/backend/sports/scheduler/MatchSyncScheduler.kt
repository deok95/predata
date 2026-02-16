package com.predata.backend.sports.scheduler

import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.event.MatchCancelledEvent
import com.predata.backend.sports.event.MatchFinishedEvent
import com.predata.backend.sports.event.MatchGoalEvent
import com.predata.backend.sports.provider.football.FootballDataProvider
import com.predata.backend.sports.repository.LeagueRepository
import com.predata.backend.sports.repository.MatchRepository
import org.slf4j.LoggerFactory
import com.predata.backend.sports.service.MatchQuestionGeneratorService
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
    private val eventPublisher: ApplicationEventPublisher,
    private val matchQuestionGeneratorService: MatchQuestionGeneratorService
) {

    private val logger = LoggerFactory.getLogger(MatchSyncScheduler::class.java)

    /**
     * 매일 06:00 → PL 경기 동기화 (신규 insert, 기존 update)
     */
    @Scheduled(cron = "\${sports.scheduler.sync-cron}")
    @Transactional
    fun syncUpcomingMatches(): MatchSyncResult {
        logger.info("[MatchSync] 매일 동기화 시작")

        val league = leagueRepository.findByExternalLeagueIdAndProvider(
            "PL", FootballDataProvider.PROVIDER_NAME
        )
        if (league == null) {
            logger.warn("[MatchSync] PL 리그를 찾을 수 없습니다. (provider=${FootballDataProvider.PROVIDER_NAME})")
            return MatchSyncResult(
                fetched = 0,
                inserted = 0,
                updated = 0,
                questionCreated = 0,
                questionSkipped = 0
            )
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

        // 동기화 후 Question 자동 생성
        val genResult = matchQuestionGeneratorService.generateQuestions()
        logger.info("[MatchSync] Question 자동 생성 - 신규: ${genResult.created}건, 스킵: ${genResult.skipped}건")

        return MatchSyncResult(
            fetched = fetched.size,
            inserted = inserted,
            updated = updated,
            questionCreated = genResult.created,
            questionSkipped = genResult.skipped
        )
    }

    /**
     * LIVE 경기 존재 시 1분 간격 폴링
     * LIVE 경기 없으면 폴링 안 함
     */
    @Scheduled(fixedDelayString = "\${sports.scheduler.live-poll-interval-ms}")
    @Transactional
    fun pollLiveMatches(): LivePollResult {
        val now = LocalDateTime.now()
        val candidates = matchRepository.findPollCandidates(now)

        if (candidates.isEmpty()) {
            logger.debug("[MatchSync] LIVE 폴링 - 대상 경기 없음, skip")
            return LivePollResult(polled = 0, updated = 0, goalEvents = 0, finishedEvents = 0, cancelledEvents = 0)
        }

        logger.info("[MatchSync] LIVE 폴링 시작 - 대상 경기: ${candidates.size}건")
        var updated = 0
        var goalEvents = 0
        var finishedEvents = 0
        var cancelledEvents = 0

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
                updated++

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
                    goalEvents++
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
                    finishedEvents++
                }

                // POSTPONED/CANCELLED 전환 감지 → MatchCancelledEvent 발행
                if (oldStatus != MatchStatus.POSTPONED && oldStatus != MatchStatus.CANCELLED
                    && (newStatus == MatchStatus.POSTPONED || newStatus == MatchStatus.CANCELLED)
                ) {
                    logger.info(
                        "[MatchSync] 경기 취소/연기: {} vs {} → {}",
                        match.homeTeam, match.awayTeam, newStatus
                    )
                    eventPublisher.publishEvent(
                        MatchCancelledEvent(
                            source = this,
                            matchId = match.id!!,
                            matchStatus = newStatus
                        )
                    )
                    cancelledEvents++
                }

                // API rate limit 방지
                Thread.sleep(400)

            } catch (e: Exception) {
                logger.error("[MatchSync] 폴링 실패: {} vs {} - {}", match.homeTeam, match.awayTeam, e.message)
            }
        }

        return LivePollResult(
            polled = candidates.size,
            updated = updated,
            goalEvents = goalEvents,
            finishedEvents = finishedEvents,
            cancelledEvents = cancelledEvents
        )
    }
}

data class MatchSyncResult(
    val fetched: Int,
    val inserted: Int,
    val updated: Int,
    val questionCreated: Int,
    val questionSkipped: Int
)

data class LivePollResult(
    val polled: Int,
    val updated: Int,
    val goalEvents: Int,
    val finishedEvents: Int,
    val cancelledEvents: Int
)
