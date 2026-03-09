package com.predata.backend.service.bot

import com.predata.backend.domain.policy.BotSchedulerPolicy
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BotScheduler(
    private val botMemberService: BotMemberService,
    private val botTradingService: BotTradingService,
    @Value("\${app.bot.enabled:true}")
    private val botEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(BotScheduler::class.java)

    @PostConstruct
    fun init() {
        if (!BotSchedulerPolicy.shouldRun(botEnabled)) {
            logger.info("[Bot] Bot system disabled")
            return
        }

        val bots = botMemberService.ensureBotMembers()
        logger.info("[Bot] ${bots.size} bot members ready")
    }

    @Scheduled(fixedDelay = 30000)
    fun runBotVoting() {
        if (!BotSchedulerPolicy.shouldRun(botEnabled)) return

        try {
            botTradingService.executeBotVoting()
        } catch (e: Exception) {
            logger.error("[Bot] Voting failed: ${e.message}")
        }
    }

    @Scheduled(fixedDelay = 60000)
    fun runBotTrading() {
        if (!BotSchedulerPolicy.shouldRun(botEnabled)) return

        try {
            botTradingService.executeBotTrading()
        } catch (e: Exception) {
            logger.error("[Bot] Trading failed: ${e.message}")
        }
    }
}
