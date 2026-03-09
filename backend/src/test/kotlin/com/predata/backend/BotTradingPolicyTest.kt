package com.predata.backend

import com.predata.backend.domain.Choice
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.policy.BotTradingPolicy
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotTradingPolicyTest {
    @Test
    fun `bot count and trade amount are within configured bounds`() {
        repeat(50) {
            val votingCount = BotTradingPolicy.pickVotingBotCount(Random(it))
            val tradingCount = BotTradingPolicy.pickTradingBotCount(Random(it))
            val amount = BotTradingPolicy.pickTradeAmount(Random(it))

            assertTrue(votingCount in 5..15)
            assertTrue(tradingCount in 3..8)
            assertTrue(amount.toInt() in 10..500)
        }
    }

    @Test
    fun `choice and outcome are valid enums`() {
        val choice = BotTradingPolicy.pickChoice(Random(1))
        val outcome = BotTradingPolicy.pickOutcome(Random(2))

        assertTrue(choice == Choice.YES || choice == Choice.NO)
        assertTrue(outcome == ShareOutcome.YES || outcome == ShareOutcome.NO)
    }

    @Test
    fun `commit hash is deterministic for same input`() {
        val h1 = BotTradingPolicy.generateCommitHash(1L, 2L, Choice.YES, "salt")
        val h2 = BotTradingPolicy.generateCommitHash(1L, 2L, Choice.YES, "salt")

        assertEquals(h1, h2)
        assertEquals(64, h1.length)
        assertEquals("1_2", BotTradingPolicy.voteCommitKey(1L, 2L))
    }
}
