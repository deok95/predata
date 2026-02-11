package com.predata.backend.sports.event

import com.predata.backend.service.BettingSuspensionService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MatchEventListener(
    private val bettingSuspensionService: BettingSuspensionService
) {

    private val logger = LoggerFactory.getLogger(MatchEventListener::class.java)

    @EventListener
    fun onMatchGoal(event: MatchGoalEvent) {
        logger.info(
            "[MatchEvent] 골 이벤트 수신 - matchId={}, score={}-{}, minute={}",
            event.matchId, event.homeScore, event.awayScore, event.minute
        )
        bettingSuspensionService.suspendBettingForMatch(event.matchId)
    }

    @EventListener
    fun onMatchFinished(event: MatchFinishedEvent) {
        logger.info(
            "[MatchEvent] 경기 종료 이벤트 수신 - matchId={}, finalScore={}-{}",
            event.matchId, event.homeScore, event.awayScore
        )
    }
}
