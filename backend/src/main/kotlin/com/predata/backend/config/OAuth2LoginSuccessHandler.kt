package com.predata.backend.config

import com.predata.backend.domain.Member
import com.predata.backend.repository.MemberRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2LoginSuccessHandler(
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val googleId = oAuth2User.getAttribute<String>("sub") ?: ""
        val email = oAuth2User.getAttribute<String>("email")?.lowercase() ?: ""
        val name = oAuth2User.getAttribute<String>("name") ?: ""

        try {
            // 1. 기존 Google 사용자 확인
            val existingByGoogleId = memberRepository.findByGoogleId(googleId)
            if (existingByGoogleId.isPresent) {
                val member = existingByGoogleId.get()
                val jwt = jwtUtil.generateToken(member.id!!, member.email, member.role)
                redirectToFrontend(response, jwt, member.id!!, false)
                return
            }

            // 2. 이메일로 기존 계정 확인 (계정 연동)
            val existingByEmail = memberRepository.findByEmail(email)
            if (existingByEmail.isPresent) {
                val member = existingByEmail.get()
                member.googleId = googleId
                memberRepository.save(member)

                val jwt = jwtUtil.generateToken(member.id!!, member.email, member.role)
                redirectToFrontend(response, jwt, member.id!!, false)
                return
            }

            // 3. 신규 사용자 → 추가 정보 필요
            redirectToFrontend(response, null, null, true, googleId, email, name)
        } catch (e: Exception) {
            val errorMessage = URLEncoder.encode("Google 로그인 실패: ${e.message}", StandardCharsets.UTF_8)
            response.sendRedirect("http://localhost:3000/auth/google/callback?error=$errorMessage")
        }
    }

    private fun redirectToFrontend(
        response: HttpServletResponse,
        token: String?,
        memberId: Long?,
        needsAdditionalInfo: Boolean,
        googleId: String? = null,
        email: String? = null,
        name: String? = null
    ) {
        val redirectUrl = if (needsAdditionalInfo) {
            val encodedEmail = URLEncoder.encode(email ?: "", StandardCharsets.UTF_8)
            val encodedName = URLEncoder.encode(name ?: "", StandardCharsets.UTF_8)
            "http://localhost:3000/auth/google/complete?googleId=$googleId&email=$encodedEmail&name=$encodedName"
        } else {
            "http://localhost:3000/auth/google/callback?token=$token&memberId=$memberId"
        }
        response.sendRedirect(redirectUrl)
    }
}
