package com.predata.backend.util

import jakarta.servlet.http.HttpServletRequest

object IpUtil {
    fun extractClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) return forwarded.split(",").first().trim()
        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) return realIp.trim()
        return request.remoteAddr ?: "unknown"
    }
}
