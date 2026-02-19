package com.predata.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.exception.ErrorResponse
import com.predata.backend.repository.MemberRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 밴 유저 활동 차단 인터셉터
 * - 요청 본문의 memberId를 확인하여 밴 여부 체크
 * - 밴된 유저는 투표/베팅 불가
 */
@Component
class BanCheckInterceptor(
    private val memberRepository: MemberRepository,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(BanCheckInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // POST 요청만 체크 (GET은 조회이므로 허용)
        if (request.method != "POST") return true

        // memberId 추출 시도 (request attribute로 전달 — 컨트롤러에서 체크하는 것이 더 적합하지만,
        // 인터셉터에서는 body를 읽으면 컨트롤러에서 못 읽으므로 헤더 기반으로 체크)
        val memberIdHeader = request.getHeader("X-Member-Id")
        if (memberIdHeader.isNullOrBlank()) return true

        val memberId = memberIdHeader.toLongOrNull() ?: return true

        val member = memberRepository.findById(memberId).orElse(null) ?: return true

        if (member.isBanned) {
            logger.warn("Banned member attempted action: memberId=$memberId, reason=${member.banReason}")

            response.status = HttpStatus.FORBIDDEN.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"

            val error = ErrorResponse(
                code = "ACCOUNT_BANNED",
                message = "Account has been suspended. Reason: ${member.banReason ?: "Terms of Service violation"}",
                status = 403
            )
            response.writer.write(objectMapper.writeValueAsString(error))
            return false
        }

        return true
    }
}
