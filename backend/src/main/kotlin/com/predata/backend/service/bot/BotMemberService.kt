package com.predata.backend.service.bot

import com.predata.backend.domain.Gender
import com.predata.backend.domain.Member
import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BotMemberService(
    private val memberRepository: MemberRepository
) {
    companion object {
        const val BOT_EMAIL_SUFFIX = "@predata.bot"
        const val BOT_COUNT = 50
        val COUNTRIES = listOf("KR", "US", "JP", "GB", "DE", "SG", "FR", "AU")
        val JOBS = listOf("TECH", "FINANCE", "HEALTHCARE", "EDUCATION", "MARKETING", "LEGAL", "MEDIA", "OTHER")
        val AGE_GROUPS = listOf(20, 25, 30, 35, 40, 45, 50)
    }

    @Transactional
    fun ensureBotMembers(): List<Member> {
        val existing = memberRepository.findAll().filter { it.email.endsWith(BOT_EMAIL_SUFFIX) }
        if (existing.size >= BOT_COUNT) return existing

        val toCreate = BOT_COUNT - existing.size
        val newBots = (1..toCreate).map { i ->
            val num = existing.size + i
            Member(
                email = "bot_${String.format("%03d", num)}$BOT_EMAIL_SUFFIX",
                countryCode = COUNTRIES.random(),
                jobCategory = JOBS.random(),
                ageGroup = AGE_GROUPS.random(),
                gender = listOf(Gender.MALE, Gender.FEMALE).random(),
                usdcBalance = BigDecimal("1000000"),
                role = "BOT"
            )
        }

        return existing + memberRepository.saveAll(newBots)
    }

    fun getBotMembers(): List<Member> {
        return memberRepository.findAll().filter { it.email.endsWith(BOT_EMAIL_SUFFIX) }
    }

    fun isBot(memberId: Long): Boolean {
        return memberRepository.findById(memberId).orElse(null)?.role == "BOT"
    }
}
