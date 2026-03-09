package com.predata.backend

import com.predata.backend.service.bot.BotMemberService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BotMemberServiceTest {

    @Autowired
    private lateinit var botMemberService: BotMemberService

    @Test
    fun `ensureBotMembers creates target bots once and remains idempotent`() {
        val first = botMemberService.ensureBotMembers()
        assertEquals(100, first.size)
        assertTrue(first.all { it.email.endsWith("@predata.bot") })
        assertTrue(first.all { it.role == "BOT" })

        val second = botMemberService.ensureBotMembers()
        assertEquals(100, second.size)
        assertTrue(second.all { it.email.endsWith("@predata.bot") })
    }
}
