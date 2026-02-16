package com.predata.backend.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2LoginFailureHandler(
    @Value("\${app.frontend-url:http://localhost:3000}")
    private val frontendUrl: String
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        // authorization_request_not_found: OAuth state 세션 유실 (서버 재시작 등)
        val errorCode = if (exception.message?.contains("authorization_request_not_found") == true) {
            "session_expired"
        } else {
            "login_failed"
        }

        val errorMessage = URLEncoder.encode(
            if (errorCode == "session_expired") "로그인을 다시 시도해주세요" else "Google 로그인에 실패했습니다",
            StandardCharsets.UTF_8
        )

        response.sendRedirect("$frontendUrl/auth/google/callback?error=$errorCode&message=$errorMessage")
    }
}
