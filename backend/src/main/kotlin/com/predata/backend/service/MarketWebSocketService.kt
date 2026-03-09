package com.predata.backend.service

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * WebSocket broadcast service for real-time market updates.
 *
 * Topics:
 *  /topic/pool/{questionId}  — pool price update after each swap
 *  /topic/markets            — any pool update (triggers market list refresh)
 *  /topic/votes/{questionId} — vote counts update after each vote
 */
@Service
class MarketWebSocketService(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    data class PoolUpdate(
        val questionId: Long,
        val yesPrice: Double,
        val noPrice: Double,
        val yesShares: String,
        val noShares: String,
    )

    data class VoteUpdate(
        val questionId: Long,
        val yesVotes: Long,
        val noVotes: Long,
        val totalVotes: Long,
    )

    fun broadcastPoolUpdate(questionId: Long, yesShares: BigDecimal, noShares: BigDecimal) {
        val total = yesShares.add(noShares)
        if (total <= BigDecimal.ZERO) return
        val yesPrice = noShares.divide(total, 6, RoundingMode.HALF_UP).toDouble()
        val update = PoolUpdate(
            questionId = questionId,
            yesPrice = yesPrice,
            noPrice = (1.0 - yesPrice),
            yesShares = yesShares.toPlainString(),
            noShares = noShares.toPlainString(),
        )
        messagingTemplate.convertAndSend("/topic/pool/$questionId", update)
        messagingTemplate.convertAndSend("/topic/markets", update)
    }

    fun broadcastVoteUpdate(questionId: Long, yesVotes: Long, noVotes: Long) {
        val update = VoteUpdate(
            questionId = questionId,
            yesVotes = yesVotes,
            noVotes = noVotes,
            totalVotes = yesVotes + noVotes,
        )
        messagingTemplate.convertAndSend("/topic/votes/$questionId", update)
        messagingTemplate.convertAndSend("/topic/votes", update)
    }
}
