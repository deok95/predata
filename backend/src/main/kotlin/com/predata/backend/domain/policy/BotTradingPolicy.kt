package com.predata.backend.domain.policy

import com.predata.backend.domain.Choice
import com.predata.backend.domain.ShareOutcome
import java.math.BigDecimal
import java.security.MessageDigest
import kotlin.random.Random

object BotTradingPolicy {
    private const val minVotingBots = 5
    private const val maxVotingBots = 15
    private const val minTradingBots = 3
    private const val maxTradingBots = 8
    private const val minTradeAmount = 10
    private const val maxTradeAmount = 500

    fun pickVotingBotCount(random: Random = Random.Default): Int = random.nextInt(minVotingBots, maxVotingBots + 1)

    fun pickTradingBotCount(random: Random = Random.Default): Int = random.nextInt(minTradingBots, maxTradingBots + 1)

    fun pickVotingBotCount(totalBots: Int, random: Random = Random.Default): Int {
        if (totalBots <= 0) return 0
        val min = (totalBots * 0.35).toInt().coerceAtLeast(10).coerceAtMost(totalBots)
        val max = (totalBots * 0.60).toInt().coerceAtLeast(min).coerceAtMost(totalBots)
        return random.nextInt(min, max + 1)
    }

    fun pickTradingBotCount(totalBots: Int, random: Random = Random.Default): Int {
        if (totalBots <= 0) return 0
        val min = (totalBots * 0.20).toInt().coerceAtLeast(8).coerceAtMost(totalBots)
        val max = (totalBots * 0.40).toInt().coerceAtLeast(min).coerceAtMost(totalBots)
        return random.nextInt(min, max + 1)
    }

    fun pickChoice(random: Random = Random.Default): Choice = if (random.nextBoolean()) Choice.YES else Choice.NO

    fun pickOutcome(random: Random = Random.Default): ShareOutcome = if (random.nextBoolean()) ShareOutcome.YES else ShareOutcome.NO

    fun pickTradeAmount(random: Random = Random.Default): BigDecimal =
        BigDecimal.valueOf(random.nextInt(minTradeAmount, maxTradeAmount + 1).toLong())

    fun voteCommitKey(questionId: Long, memberId: Long): String = "${questionId}_${memberId}"

    fun generateCommitHash(questionId: Long, memberId: Long, choice: Choice, salt: String): String {
        val input = "$questionId:$memberId:${choice.name}:$salt"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
