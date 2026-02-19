package com.predata.backend.service

import com.predata.backend.config.JwtUtil
import com.predata.backend.domain.EmailVerification
import com.predata.backend.domain.Member
import com.predata.backend.repository.EmailVerificationRepository
import com.predata.backend.repository.MemberRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val emailVerificationRepository: EmailVerificationRepository,
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil,
    private val emailService: EmailService
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    companion object {
        private const val CODE_EXPIRY_MINUTES = 5L
        private const val MAX_ATTEMPTS = 5
    }

    // Temporary storage (use Redis in production)
    private val verifiedEmails = mutableMapOf<String, String>() // email -> code (verified)

    /**
     * Step 1: Send verification code to email
     */
    @Transactional
    fun sendVerificationCode(email: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        // Check if email is already registered
        if (memberRepository.existsByEmail(normalizedEmail)) {
            return mapOf(
                "success" to false,
                "message" to "Email already registered."
            )
        }

        val code = (100000..999999).random().toString()

        val verification = EmailVerification(
            email = normalizedEmail,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES)
        )
        emailVerificationRepository.save(verification)

        // Send email
        emailService.sendVerificationCode(normalizedEmail, code)

        return mapOf(
            "success" to true,
            "message" to "Verification code sent to email. (Enter within ${CODE_EXPIRY_MINUTES} minutes)",
            "expiresInSeconds" to CODE_EXPIRY_MINUTES * 60
        )
    }

    /**
     * Step 2: Verify code (does not create member)
     */
    @Transactional
    fun verifyCode(email: String, code: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        val verification = emailVerificationRepository
            .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
            .orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "Verification code not found."
            )

        if (verification.verified) {
            return mapOf(
                "success" to false,
                "message" to "Verification code already used."
            )
        }

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            return mapOf(
                "success" to false,
                "message" to "Verification code expired."
            )
        }

        if (verification.attempts >= MAX_ATTEMPTS) {
            return mapOf(
                "success" to false,
                "message" to "Verification attempt limit exceeded."
            )
        }

        verification.attempts++

        if (verification.code != code) {
            emailVerificationRepository.save(verification)
            val remaining = MAX_ATTEMPTS - verification.attempts
            return mapOf(
                "success" to false,
                "message" to "Verification code does not match. (${remaining} attempts remaining)"
            )
        }

        // Verification success - set verified flag and store temporarily
        verification.verified = true
        emailVerificationRepository.save(verification)
        verifiedEmails[normalizedEmail] = code

        return mapOf(
            "success" to true,
            "message" to "Verification code confirmed."
        )
    }

    /**
     * Step 3: Set password and create member
     */
    @Transactional
    fun completeSignup(
        email: String,
        code: String,
        password: String,
        countryCode: String = "KR",
        gender: String? = null,
        birthDate: String? = null,
        jobCategory: String? = null,
        ageGroup: Int? = null
    ): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        // Check if email is verified (check from DB)
        val verification = emailVerificationRepository
            .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
            .orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "Verification code not found."
            )

        // Check if verification is complete
        if (!verification.verified) {
            return mapOf(
                "success" to false,
                "message" to "Email not verified. Please verify code first."
            )
        }

        // Check code match
        if (verification.code != code) {
            return mapOf(
                "success" to false,
                "message" to "Verification code does not match."
            )
        }

        // Check expiration
        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            return mapOf(
                "success" to false,
                "message" to "Verification code expired. Please try again."
            )
        }

        // Check if email is already registered (double check)
        if (memberRepository.existsByEmail(normalizedEmail)) {
            verifiedEmails.remove(normalizedEmail)
            return mapOf(
                "success" to false,
                "message" to "Email already registered."
            )
        }

        // Parse optional fields
        val genderEnum = gender?.let {
            try {
                com.predata.backend.domain.Gender.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        val birthDateParsed = birthDate?.let {
            try {
                java.time.LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        // Create member
        val passwordHash = passwordEncoder.encode(password)
        val member = Member(
            email = normalizedEmail,
            passwordHash = passwordHash,
            countryCode = countryCode,
            gender = genderEnum,
            birthDate = birthDateParsed,
            jobCategory = jobCategory,
            ageGroup = ageGroup,
            tier = "BRONZE"
        )
        val savedMember = memberRepository.save(member)

        // Remove from temporary storage (if exists)
        verifiedEmails.remove(normalizedEmail)

        // Issue JWT token (auto login)
        val token = jwtUtil.generateToken(savedMember.id!!, savedMember.email, savedMember.role)

        return mapOf(
            "success" to true,
            "message" to "Registration completed.",
            "token" to token,
            "memberId" to savedMember.id
        )
    }

    /**
     * Login
     */
    @Transactional(readOnly = true)
    fun login(email: String, password: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        val member = memberRepository.findByEmail(normalizedEmail).orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "Email or password does not match."
            )

        // Check if password hash exists
        if (member.passwordHash == null) {
            return mapOf(
                "success" to false,
                "message" to "Account without password. Please login with wallet or reset password."
            )
        }

        // Verify password
        if (!passwordEncoder.matches(password, member.passwordHash)) {
            return mapOf(
                "success" to false,
                "message" to "Email or password does not match."
            )
        }

        // Check ban status
        if (member.isBanned) {
            return mapOf(
                "success" to false,
                "message" to "Account suspended. Reason: ${member.banReason ?: "Please contact administrator."}"
            )
        }

        // Issue JWT token
        val token = jwtUtil.generateToken(member.id!!, member.email, member.role)

        return mapOf(
            "success" to true,
            "message" to "Login successful",
            "token" to token,
            "memberId" to member.id
        )
    }
}
