package com.predata.backend

import com.predata.backend.domain.policy.BotMemberPolicy
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BotMemberPolicyTest {
    @Test
    fun `bot email and count rules are deterministic`() {
        assertTrue(BotMemberPolicy.isBotEmail("bot_001@predata.bot"))
        assertFalse(BotMemberPolicy.isBotEmail("user@example.com"))
        assertEquals(40, BotMemberPolicy.missingBotCount(60))
        assertEquals(55, BotMemberPolicy.missingBotCount(45))
        assertEquals("bot_007@predata.bot", BotMemberPolicy.botEmail(7))
    }

    @Test
    fun `created bot member uses bot role and suffix`() {
        val bot = BotMemberPolicy.createBotMember(sequence = 1, random = Random(1))
        assertTrue(bot.email.endsWith("@predata.bot"))
        assertEquals("BOT", bot.role)
        assertEquals("1000000", bot.usdcBalance.stripTrailingZeros().toPlainString())
        assertTrue(BotMemberPolicy.isBotRole(bot.role))
    }
}
