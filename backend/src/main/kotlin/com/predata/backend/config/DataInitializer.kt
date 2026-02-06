package com.predata.backend.config

import com.predata.backend.domain.Member
import com.predata.backend.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val memberRepository: MemberRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()

    companion object {
        const val ADMIN_EMAIL = "admin@predata.io"
        const val ADMIN_PASSWORD = "123400"
    }

    override fun run(vararg args: String?) {
        createAdminIfNotExists()
    }

    private fun createAdminIfNotExists() {
        if (memberRepository.existsByEmail(ADMIN_EMAIL)) {
            logger.info("[DataInitializer] Admin account already exists: $ADMIN_EMAIL")
            return
        }

        val admin = Member(
            email = ADMIN_EMAIL,
            passwordHash = passwordEncoder.encode(ADMIN_PASSWORD),
            countryCode = "KR",
            tier = "DIAMOND",
            role = "ADMIN",
            pointBalance = 1000000
        )

        memberRepository.save(admin)
        logger.info("[DataInitializer] Admin account created: $ADMIN_EMAIL")
    }
}
