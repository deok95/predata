package com.predata.backend

import com.predata.backend.repository.EmailVerificationRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.service.AuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthE2ETest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var emailVerificationRepository: EmailVerificationRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Test
    fun `auth e2e - signup 3 steps, login, and error cases`() {
        val email = "auth-e2e-${System.currentTimeMillis()}@example.com"
        val password = "StrongPass123!"

        // 1) sendVerificationCode -> success=true, expiresInSeconds 존재
        val sendResult = authService.sendVerificationCode(email)
        assertTrue(sendResult.bool("success"), "sendVerificationCode should succeed")
        assertNotNull(sendResult["expiresInSeconds"], "expiresInSeconds should exist")

        val verification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow { IllegalStateException("verification code not found in DB") }
        val correctCode = verification.code

        // 2) verifyCode 잘못된 코드 -> success=false
        val wrongVerifyResult = authService.verifyCode(email, "000000")
        assertFalse(wrongVerifyResult.bool("success"), "verifyCode with wrong code should fail")

        // 3) verifyCode 올바른 코드 -> success=true
        val verifyResult = authService.verifyCode(email, correctCode)
        assertTrue(verifyResult.bool("success"), "verifyCode with correct code should succeed")

        // 4) completeSignup -> success=true, token/memberId 존재
        val signupResult = authService.completeSignup(
            email = email,
            code = correctCode,
            password = password,
            countryCode = "KR"
        )
        assertTrue(signupResult.bool("success"), "completeSignup should succeed")
        assertNotNull(signupResult["token"], "signup token should exist")
        assertNotNull(signupResult["memberId"], "signup memberId should exist")

        // 5) 동일 이메일 재가입 시도 -> success=false
        val duplicateSignupResult = authService.completeSignup(
            email = email,
            code = correctCode,
            password = password,
            countryCode = "KR"
        )
        assertFalse(duplicateSignupResult.bool("success"), "duplicate signup should fail")

        // 6) login 올바른 비밀번호 -> success=true, token 존재
        val loginSuccessResult = authService.login(email, password)
        assertTrue(loginSuccessResult.bool("success"), "login with correct password should succeed")
        assertNotNull(loginSuccessResult["token"], "login token should exist")

        // 7) login 잘못된 비밀번호 -> success=false
        val loginWrongPasswordResult = authService.login(email, "WrongPass123!")
        assertFalse(loginWrongPasswordResult.bool("success"), "login with wrong password should fail")

        // 8) login 밴 유저 -> success=false
        val member = memberRepository.findByEmail(email).orElseThrow()
        member.isBanned = true
        member.banReason = "E2E ban test"
        memberRepository.save(member)

        val loginBannedResult = authService.login(email, password)
        assertFalse(loginBannedResult.bool("success"), "login for banned user should fail")
    }

    private fun Map<String, Any>.bool(key: String): Boolean = this[key] as? Boolean ?: false
}
