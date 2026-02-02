package com.predata.common.events

import java.io.Serializable

// Bet Placed Event
data class BetPlacedEvent(
    val questionId: Long,
    val memberId: Long,
    val userAddress: String,
    val choice: String, // YES or NO
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

// Settlement Completed Event
data class SettlementCompletedEvent(
    val questionId: Long,
    val finalResult: String, // YES or NO
    val totalWinners: Int,
    val totalPayout: Long,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

// Question Settled Event
data class QuestionSettledEvent(
    val questionId: Long,
    val finalResult: String, // YES or NO
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

// Match Updated Event
data class MatchUpdatedEvent(
    val matchId: Long,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

// Question Created Event
data class QuestionCreatedEvent(
    val questionId: Long,
    val title: String,
    val category: String?,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
