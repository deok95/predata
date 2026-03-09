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
class AdminAuthInterceptor(
    private val objectMapper: ObjectMapper,
    private val auditService: com.predata.backend.service.AuditService
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (request.method == "OPTIONS") return true

        val role = request.getAttribute(JwtAuthInterceptor.ATTR_ROLE) as? String
        if (role != "ADMIN") {
            // Audit log: 권한 거부
            val memberId = request.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            val ipAddress = request.remoteAddr
            val requestUri = request.requestURI
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.PERMISSION_DENIED,
                entityType = "ENDPOINT",
                entityId = null,
                detail = "Admin access denied to $requestUri",
                ipAddress = ipAddress
            )

            response.status = ErrorCode.FORBIDDEN.status
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            val error = ErrorResponse.of(ErrorCode.FORBIDDEN, customMessage = "Admin privileges required.")
            response.writer.write(objectMapper.writeValueAsString(error))
            return false
        }
        return true
    }
}
