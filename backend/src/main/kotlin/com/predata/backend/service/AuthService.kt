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

    // 임시 저장소 (프로덕션에서는 Redis 등 사용)
    private val verifiedEmails = mutableMapOf<String, String>() // email -> code (verified)

    /**
     * Step 1: 이메일로 인증 코드 발송
     */
    @Transactional
    fun sendVerificationCode(email: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        // 이미 가입된 이메일 체크
        if (memberRepository.existsByEmail(normalizedEmail)) {
            return mapOf(
                "success" to false,
                "message" to "이미 가입된 이메일입니다."
            )
        }

        val code = (100000..999999).random().toString()

        val verification = EmailVerification(
            email = normalizedEmail,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES)
        )
        emailVerificationRepository.save(verification)

        // 이메일 발송
        emailService.sendVerificationCode(normalizedEmail, code)

        return mapOf(
            "success" to true,
            "message" to "인증 코드가 이메일로 발송되었습니다. (${CODE_EXPIRY_MINUTES}분 이내 입력)",
            "expiresInSeconds" to CODE_EXPIRY_MINUTES * 60
        )
    }

    /**
     * Step 2: 인증 코드 검증 (회원 생성 안 함)
     */
    @Transactional
    fun verifyCode(email: String, code: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        val verification = emailVerificationRepository
            .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
            .orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "인증 코드를 찾을 수 없습니다."
            )

        if (verification.verified) {
            return mapOf(
                "success" to false,
                "message" to "이미 사용된 인증 코드입니다."
            )
        }

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            return mapOf(
                "success" to false,
                "message" to "인증 코드가 만료되었습니다."
            )
        }

        if (verification.attempts >= MAX_ATTEMPTS) {
            return mapOf(
                "success" to false,
                "message" to "인증 시도 횟수를 초과했습니다."
            )
        }

        verification.attempts++

        if (verification.code != code) {
            emailVerificationRepository.save(verification)
            val remaining = MAX_ATTEMPTS - verification.attempts
            return mapOf(
                "success" to false,
                "message" to "인증 코드가 일치하지 않습니다. (${remaining}회 남음)"
            )
        }

        // 인증 성공 - verified 플래그 설정하고 임시 저장
        verification.verified = true
        emailVerificationRepository.save(verification)
        verifiedEmails[normalizedEmail] = code

        return mapOf(
            "success" to true,
            "message" to "인증 코드가 확인되었습니다."
        )
    }

    /**
     * Step 3: 비밀번호 설정 및 회원 생성
     */
    @Transactional
    fun completeSignup(email: String, code: String, password: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        // 인증된 이메일인지 확인 (DB에서 직접 확인)
        val verification = emailVerificationRepository
            .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
            .orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "인증 코드를 찾을 수 없습니다."
            )

        // 인증 완료 여부 확인
        if (!verification.verified) {
            return mapOf(
                "success" to false,
                "message" to "인증되지 않은 이메일입니다. 인증 코드를 먼저 확인해주세요."
            )
        }

        // 코드 일치 여부 확인
        if (verification.code != code) {
            return mapOf(
                "success" to false,
                "message" to "인증 코드가 일치하지 않습니다."
            )
        }

        // 만료 확인
        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            return mapOf(
                "success" to false,
                "message" to "인증 코드가 만료되었습니다. 다시 시도해주세요."
            )
        }

        // 이미 가입된 이메일 체크 (이중 확인)
        if (memberRepository.existsByEmail(normalizedEmail)) {
            verifiedEmails.remove(normalizedEmail)
            return mapOf(
                "success" to false,
                "message" to "이미 가입된 이메일입니다."
            )
        }

        // 회원 생성
        val passwordHash = passwordEncoder.encode(password)
        val member = Member(
            email = normalizedEmail,
            passwordHash = passwordHash,
            countryCode = "KR", // 기본값
            tier = "BRONZE"
        )
        val savedMember = memberRepository.save(member)

        // 임시 저장소에서 제거 (있다면)
        verifiedEmails.remove(normalizedEmail)

        // JWT 토큰 발급 (자동 로그인)
        val token = jwtUtil.generateToken(savedMember.id!!, savedMember.email, savedMember.role)

        return mapOf(
            "success" to true,
            "message" to "회원가입이 완료되었습니다.",
            "token" to token,
            "memberId" to savedMember.id
        )
    }

    /**
     * 로그인
     */
    @Transactional(readOnly = true)
    fun login(email: String, password: String): Map<String, Any> {
        val normalizedEmail = email.trim().lowercase()

        val member = memberRepository.findByEmail(normalizedEmail).orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "이메일 또는 비밀번호가 일치하지 않습니다."
            )

        // 비밀번호 해시 존재 여부 확인
        if (member.passwordHash == null) {
            return mapOf(
                "success" to false,
                "message" to "비밀번호가 설정되지 않은 계정입니다. 지갑으로 로그인하거나 비밀번호를 재설정하세요."
            )
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, member.passwordHash)) {
            return mapOf(
                "success" to false,
                "message" to "이메일 또는 비밀번호가 일치하지 않습니다."
            )
        }

        // 밴 체크
        if (member.isBanned) {
            return mapOf(
                "success" to false,
                "message" to "정지된 계정입니다. 사유: ${member.banReason ?: "관리자에게 문의하세요."}"
            )
        }

        // JWT 토큰 발급
        val token = jwtUtil.generateToken(member.id!!, member.email, member.role)

        return mapOf(
            "success" to true,
            "message" to "로그인 성공",
            "token" to token,
            "memberId" to member.id
        )
    }
}
