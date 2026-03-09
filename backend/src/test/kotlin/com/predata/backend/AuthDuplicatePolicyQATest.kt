package com.predata.backend

import com.predata.backend.domain.Member
import com.predata.backend.dto.CompleteGoogleRegistrationRequest
import com.predata.backend.repository.EmailVerificationRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.service.AuthService
import com.predata.backend.service.GoogleOAuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthDuplicatePolicyQATest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var googleOAuthService: GoogleOAuthService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var emailVerificationRepository: EmailVerificationRepository

    @Test
    fun `email signup then google registration links same account instead of duplicate signup`() {
        val email = "qa-auth-link-${System.currentTimeMillis()}@example.com"
        val password = "StrongPass123!"
        val googleId = "google-link-${System.currentTimeMillis()}"

        // Email signup flow
        val sendResult = authService.sendVerificationCode(email)
        assertTrue(sendResult.bool("success"))

        val code = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email).orElseThrow().code
        val verifyResult = authService.verifyCode(email, code)
        assertTrue(verifyResult.bool("success"))

        val signupResult = authService.completeSignup(email, code, password, "KR")
        assertTrue(signupResult.bool("success"))
        val emailMemberId = (signupResult["memberId"] as Number).toLong()
        val beforeCount = memberRepository.count()

        // Same email with Google complete-registration should link, not create new member
        val googleResult = googleOAuthService.completeRegistration(
            CompleteGoogleRegistrationRequest(
                googleId = googleId,
                email = email,
                countryCode = "KR",
                gender = null,
                birthDate = null,
                jobCategory = "ENGINEER",
                ageGroup = 30
            )
        )

        assertTrue(googleResult.success)
        assertEquals(emailMemberId, googleResult.memberId)
        assertEquals(beforeCount, memberRepository.count(), "member row must not increase")

        val linked = memberRepository.findById(emailMemberId).orElseThrow()
        assertEquals(googleId, linked.googleId)
    }

    @Test
    fun `same googleId cannot register twice`() {
        val googleId = "google-dup-${System.currentTimeMillis()}"

        val first = googleOAuthService.completeRegistration(
            CompleteGoogleRegistrationRequest(
                googleId = googleId,
                email = "qa-google-1-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                gender = null,
                birthDate = null,
                jobCategory = "ENGINEER",
                ageGroup = 30
            )
        )
        assertTrue(first.success)
        assertNotNull(first.memberId)

        val second = googleOAuthService.completeRegistration(
            CompleteGoogleRegistrationRequest(
                googleId = googleId,
                email = "qa-google-2-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                gender = null,
                birthDate = null,
                jobCategory = "ENGINEER",
                ageGroup = 30
            )
        )
        assertFalse(second.success)
        assertTrue(second.message.contains("already", ignoreCase = true))
    }

    @Test
    fun `wallet duplicate is blocked by unique constraint and wallet login resolves single account`() {
        val wallet = "0x1111111111111111111111111111111111111111"
        val memberA = memberRepository.save(
            Member(
                email = "qa-wallet-a-${System.currentTimeMillis()}@example.com",
                walletAddress = wallet,
                countryCode = "KR",
                jobCategory = "ENGINEER",
                ageGroup = 30
            )
        )
        val memberB = memberRepository.save(
            Member(
                email = "qa-wallet-b-${System.currentTimeMillis()}@example.com",
                walletAddress = null,
                countryCode = "KR",
                jobCategory = "ENGINEER",
                ageGroup = 30
            )
        )

        memberB.walletAddress = wallet
        try {
            memberRepository.saveAndFlush(memberB)
            fail("duplicate wallet must fail")
        } catch (_: DataIntegrityViolationException) {
            // expected
        }

        // DB unique 제약이 통과된 경우 memberA만 해당 wallet 주소를 소유해야 한다
        val byWallet = memberRepository.findByWalletAddressIgnoreCase(wallet).orElse(null)
        assertNotNull(byWallet)
        assertEquals(memberA.id, byWallet.id)

        // 미등록 wallet은 조회되지 않아야 한다
        val unknown = memberRepository.findByWalletAddressIgnoreCase("0x2222222222222222222222222222222222222222").orElse(null)
        assertNull(unknown)
    }

    private fun Map<String, Any>.bool(key: String): Boolean = this[key] as? Boolean ?: false
}
