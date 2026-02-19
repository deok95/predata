package com.predata.backend.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${auth.demo-mode:true}") val demoMode: Boolean
) {

    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendVerificationCode(to: String, code: String) {
        if (demoMode) {
            logger.info("[Email] 데모 모드 - 이메일 발송 생략 (to={}, code={})", to, code)
            return
        }

        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setTo(to)
            helper.setSubject("[Predata] 이메일 인증 코드")
            helper.setText(
                """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                    <h2 style="color: #4F46E5;">Predata 이메일 인증</h2>
                    <p style="color: #475569;">아래 인증 코드를 입력해주세요:</p>
                    <div style="background: #F1F5F9; border-radius: 12px; padding: 24px; text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: 900; letter-spacing: 8px; color: #1E293B;">$code</span>
                    </div>
                    <p style="color: #94A3B8; font-size: 14px;">이 코드는 5분간 유효합니다.</p>
                </div>
                """.trimIndent(),
                true
            )
            mailSender.send(message)
            logger.info("[Email] 인증 코드 발송 완료: to={}", to)
        } catch (e: Exception) {
            logger.error("[Email] Verification code sending failed: to={}, error={}", to, e.message)
            throw RuntimeException("Email sending failed. Please try again later.")
        }
    }
}
