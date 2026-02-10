package com.predata.backend.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2LoginFailureHandler : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val errorMessage = URLEncoder.encode(
            exception.message ?: "Google 로그인에 실패했습니다",
            StandardCharsets.UTF_8
        )
        response.sendRedirect("http://localhost:3000/auth/google/callback?error=$errorMessage")
    }
}
