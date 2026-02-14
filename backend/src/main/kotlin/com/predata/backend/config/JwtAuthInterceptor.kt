package com.predata.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.exception.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class JwtAuthInterceptor(
    private val jwtUtil: JwtUtil,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    companion object {
        const val ATTR_MEMBER_ID = "authenticatedMemberId"
        const val ATTR_EMAIL = "authenticatedEmail"
        const val ATTR_ROLE = "authenticatedRole"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method == "OPTIONS") return true

        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다.")
            return false
        }

        val token = authHeader.substring(7)
        val claims = jwtUtil.validateAndParse(token)
        if (claims == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않거나 만료된 토큰입니다.")
            return false
        }

        // Extract claims with NPE protection
        try {
            val memberId = jwtUtil.getMemberId(claims)
            val email = jwtUtil.getEmail(claims)
            val role = jwtUtil.getRole(claims)

            request.setAttribute(ATTR_MEMBER_ID, memberId)
            request.setAttribute(ATTR_EMAIL, email)
            request.setAttribute(ATTR_ROLE, role)
        } catch (e: Exception) {
            // Handle malformed JWT (missing subject, email, or role claims)
            writeError(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "토큰 형식이 올바르지 않습니다.")
            return false
        }

        return true
    }

    private fun writeError(response: HttpServletResponse, status: HttpStatus, code: String, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val error = ErrorResponse(code = code, message = message, status = status.value())
        response.writer.write(objectMapper.writeValueAsString(error))
    }
}
