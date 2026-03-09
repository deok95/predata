package com.predata.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.exception.ErrorCode
import com.predata.backend.exception.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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

        // GET /api/questions/** 공개 엔드포인트 — credits/drafts 경로는 JWT 필수
        val uri = request.requestURI
        val isCreditOrDraft = uri.startsWith("/api/questions/credits") ||
            uri.startsWith("/api/questions/drafts")
        if (request.method == "GET" && !isCreditOrDraft &&
            uri.matches(Regex("^/api/questions(/.*)?$"))
        ) {
            return true
        }

        // POST /api/questions/{id}/view 는 공개 엔드포인트 (조회수 집계) - 인증 제외
        if (request.method == "POST" && request.requestURI.matches(Regex("^/api/questions/\\d+/view$"))) {
            return true
        }

        // GET /api/members/{숫자ID} 패턴만 인증 제외 (me, by-email 등은 인증 필요)
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/members/\\d+$"))) {
            return true
        }

        // Public social read endpoints
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/users/\\d+$"))) {
            return true
        }
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/users/\\d+/(followers|following)$"))) {
            return true
        }
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/questions/\\d+/comments$"))) {
            return true
        }

        // Public payment config endpoint (chain/wallet info for frontend)
        if (request.method == "GET" && uri == "/api/payments/config") return true

        // Public AMM read endpoints (GET only)
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/pool/\\d+$"))) {
            return true
        }
        if (request.method == "GET" && request.requestURI.startsWith("/api/swap/simulate")) {
            return true
        }
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/swap/price-history/\\d+$"))) {
            return true
        }
        if (request.method == "GET" && request.requestURI.matches(Regex("^/api/swap/history/\\d+$"))) {
            return true
        }

        // GET /api/votes/status/{questionId}, /api/votes/feed — optional auth
        // - 헤더 없음: 미인증으로 통과
        // - 헤더 있음 + Bearer 아님: 명시적 401 (잘못된 포맷은 거부)
        // - Bearer: 토큰 검증 후 attributes 설정, 실패 시 401
        if (request.method == "GET" && (
            request.requestURI.matches(Regex("^/api/votes/status/\\d+$")) ||
            request.requestURI == "/api/votes/feed"
        )) {
            val authHeader = request.getHeader("Authorization")
            if (authHeader == null) return true
            if (!authHeader.startsWith("Bearer ")) {
                writeError(response, ErrorCode.INVALID_TOKEN, customMessage = "Invalid Authorization header format.")
                return false
            }
            return applyJwtAttributes(request, response, authHeader.substring(7))
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, ErrorCode.UNAUTHORIZED)
            return false
        }

        return applyJwtAttributes(request, response, authHeader.substring(7))
    }

    /**
     * JWT 토큰을 검증하고 request attributes에 인증 정보를 설정한다.
     * 유효하지 않은 토큰이면 401 응답 후 false 반환.
     */
    private fun applyJwtAttributes(
        request: HttpServletRequest,
        response: HttpServletResponse,
        token: String
    ): Boolean {
        val claims = jwtUtil.validateAndParse(token)
        if (claims == null) {
            writeError(response, ErrorCode.INVALID_TOKEN)
            return false
        }

        return try {
            request.setAttribute(ATTR_MEMBER_ID, jwtUtil.getMemberId(claims))
            request.setAttribute(ATTR_EMAIL, jwtUtil.getEmail(claims))
            request.setAttribute(ATTR_ROLE, jwtUtil.getRole(claims))
            true
        } catch (e: Exception) {
            writeError(response, ErrorCode.INVALID_TOKEN, customMessage = "Invalid token format.")
            false
        }
    }

    private fun writeError(response: HttpServletResponse, errorCode: ErrorCode, customMessage: String? = null) {
        response.status = errorCode.status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(ErrorResponse.of(errorCode, customMessage = customMessage)))
    }
}
