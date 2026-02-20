package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.bot.BotMemberService
import com.predata.backend.service.bot.BotTradingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/bot")
class BotController(
    private val botMemberService: BotMemberService,
    private val botTradingService: BotTradingService
) {
    @PostMapping("/init")
    fun initBots(): ResponseEntity<ApiEnvelope<Map<String, Any>>> {
        val bots = botMemberService.ensureBotMembers()
        return ResponseEntity.ok(
            ApiEnvelope.ok(
                mapOf(
                    "botCount" to bots.size,
                    "message" to "Bot members initialized"
                )
            )
        )
    }

    @PostMapping("/vote")
    fun triggerVoting(): ResponseEntity<ApiEnvelope<Map<String, String>>> {
        botTradingService.executeBotVoting()
        return ResponseEntity.ok(ApiEnvelope.ok(mapOf("message" to "Bot voting executed")))
    }

    @PostMapping("/trade")
    fun triggerTrading(): ResponseEntity<ApiEnvelope<Map<String, String>>> {
        botTradingService.executeBotTrading()
        return ResponseEntity.ok(ApiEnvelope.ok(mapOf("message" to "Bot trading executed")))
    }
}
