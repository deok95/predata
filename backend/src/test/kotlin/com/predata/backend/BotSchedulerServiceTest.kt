package com.predata.backend

import com.predata.backend.service.bot.BotMemberService
import com.predata.backend.service.bot.BotScheduler
import com.predata.backend.service.bot.BotTradingService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BotSchedulerServiceTest {
    @Test
    fun `init and scheduled jobs do nothing when bot is disabled`() {
        val botMemberService = mock<BotMemberService>()
        val botTradingService = mock<BotTradingService>()
        val scheduler = BotScheduler(botMemberService, botTradingService, false)

        scheduler.init()
        scheduler.runBotVoting()
        scheduler.runBotTrading()

        verify(botMemberService, never()).ensureBotMembers()
        verify(botTradingService, never()).executeBotVoting()
        verify(botTradingService, never()).executeBotTrading()
    }

    @Test
    fun `init prepares bots and scheduled jobs call trading services when enabled`() {
        val botMemberService = mock<BotMemberService>()
        val botTradingService = mock<BotTradingService>()
        whenever(botMemberService.ensureBotMembers()).thenReturn(emptyList())

        val scheduler = BotScheduler(botMemberService, botTradingService, true)
        scheduler.init()
        scheduler.runBotVoting()
        scheduler.runBotTrading()

        verify(botMemberService, times(1)).ensureBotMembers()
        verify(botTradingService, times(1)).executeBotVoting()
        verify(botTradingService, times(1)).executeBotTrading()
    }

    @Test
    fun `scheduled jobs swallow runtime exceptions`() {
        val botMemberService = mock<BotMemberService>()
        val botTradingService = mock<BotTradingService>()
        whenever(botTradingService.executeBotVoting()).thenThrow(RuntimeException("vote error"))
        whenever(botTradingService.executeBotTrading()).thenThrow(RuntimeException("trade error"))

        val scheduler = BotScheduler(botMemberService, botTradingService, true)
        scheduler.runBotVoting()
        scheduler.runBotTrading()

        verify(botTradingService, times(1)).executeBotVoting()
        verify(botTradingService, times(1)).executeBotTrading()
    }
}
