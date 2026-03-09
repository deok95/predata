package com.predata.backend.domain.policy

import com.predata.backend.domain.Gender
import com.predata.backend.domain.Member
import java.math.BigDecimal
import kotlin.random.Random

object BotMemberPolicy {
    const val botEmailSuffix: String = "@predata.bot"
    const val targetBotCount: Int = 100
    private val countries = listOf("KR", "US", "JP", "GB", "DE", "SG", "FR", "AU")
    private val jobs = listOf("TECH", "FINANCE", "HEALTHCARE", "EDUCATION", "MARKETING", "LEGAL", "MEDIA", "OTHER")
    private val ageGroups = listOf(20, 25, 30, 35, 40, 45, 50)
    private val genders = listOf(Gender.MALE, Gender.FEMALE)
    private val botUsdcBalance = BigDecimal("1000000")
    private const val botRole = "BOT"

    fun isBotEmail(email: String): Boolean = email.endsWith(botEmailSuffix)

    fun missingBotCount(existingCount: Int): Int = (targetBotCount - existingCount).coerceAtLeast(0)

    fun botEmail(sequence: Int): String = "bot_${sequence.toString().padStart(3, '0')}$botEmailSuffix"

    fun createBotMember(sequence: Int, random: Random = Random.Default): Member {
        return Member(
            email = botEmail(sequence),
            countryCode = countries.random(random),
            jobCategory = jobs.random(random),
            ageGroup = ageGroups.random(random),
            gender = genders.random(random),
            usdcBalance = botUsdcBalance,
            role = botRole
        )
    }

    fun isBotRole(role: String?): Boolean = role == botRole
}
