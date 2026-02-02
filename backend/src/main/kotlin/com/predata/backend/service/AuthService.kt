package com.predata.backend.service

import com.predata.backend.domain.EmailVerification
import com.predata.backend.repository.EmailVerificationRepository
import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val emailVerificationRepository: EmailVerificationRepository,
    private val memberRepository: MemberRepository
) {

    companion object {
        private const val CODE_EXPIRY_MINUTES = 5L
        private const val MAX_ATTEMPTS = 5
    }

    /**
     * 인증 코드 발송
     * TODO: 프로덕션에서는 실제 이메일 발송 서비스로 교체
     */
    @Transactional
    fun sendVerificationCode(email: String): SendCodeResponse {
        val normalizedEmail = email.trim().lowercase()
        val code = (100000..999999).random().toString()

        val verification = EmailVerification(
            email = normalizedEmail,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES)
        )
        emailVerificationRepository.save(verification)

        return SendCodeResponse(
            success = true,
            message = "인증 코드가 발송되었습니다. (${CODE_EXPIRY_MINUTES}분 이내 입력)",
            expiresInSeconds = CODE_EXPIRY_MINUTES * 60,
            code = code // 데모 전용 — 프로덕션에서 제거
        )
    }

    /**
     * 인증 코드 검증
     * 성공 시 기존 회원 여부도 함께 반환
     */
    @Transactional
    fun verifyCode(email: String, code: String): VerifyCodeResponse {
        val normalizedEmail = email.trim().lowercase()

        val verification = emailVerificationRepository
            .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
            .orElse(null)
            ?: return VerifyCodeResponse(
                success = false,
                message = "인증 코드를 찾을 수 없습니다. 다시 요청해주세요."
            )

        if (verification.verified) {
            return VerifyCodeResponse(
                success = false,
                message = "이미 사용된 인증 코드입니다. 다시 요청해주세요."
            )
        }

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            return VerifyCodeResponse(
                success = false,
                message = "인증 코드가 만료되었습니다. 다시 요청해주세요."
            )
        }

        if (verification.attempts >= MAX_ATTEMPTS) {
            return VerifyCodeResponse(
                success = false,
                message = "인증 시도 횟수를 초과했습니다. 다시 요청해주세요."
            )
        }

        verification.attempts++

        if (verification.code != code) {
            emailVerificationRepository.save(verification)
            val remaining = MAX_ATTEMPTS - verification.attempts
            return VerifyCodeResponse(
                success = false,
                message = "인증 코드가 일치하지 않습니다. (${remaining}회 남음)"
            )
        }

        verification.verified = true
        emailVerificationRepository.save(verification)

        // 기존 회원 확인
        val member = memberRepository.findByEmail(normalizedEmail).orElse(null)

        return VerifyCodeResponse(
            success = true,
            verified = true,
            isNewUser = member == null,
            memberId = member?.id,
            message = if (member != null) "인증 완료! 로그인되었습니다." else "인증 완료! 회원가입을 진행해주세요."
        )
    }
}

data class SendCodeResponse(
    val success: Boolean,
    val message: String,
    val expiresInSeconds: Long = 300,
    val code: String? = null // 데모 전용
)

data class VerifyCodeResponse(
    val success: Boolean,
    val verified: Boolean = false,
    val isNewUser: Boolean = false,
    val memberId: Long? = null,
    val message: String
)
