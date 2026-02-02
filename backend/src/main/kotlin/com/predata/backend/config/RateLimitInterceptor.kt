package com.predata.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.exception.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * IP 기반 API Rate Limiting
 * - 일반 API: 60req/분
 * - 투표/베팅: 10req/분 (어뷰징 방지 강화)
 * - 회원가입: 5req/분 (다중계정 방지)
 */
@Component
class RateLimitInterceptor(
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(RateLimitInterceptor::class.java)

    // IP별 요청 카운터: key = "IP:bucket", value = (count, windowStart)
    private val requestCounts = ConcurrentHashMap<String, RateLimitBucket>()

    companion object {
        const val GENERAL_LIMIT = 60        // 일반: 60req/분
        const val ACTIVITY_LIMIT = 10       // 투표/베팅: 10req/분
        const val SIGNUP_LIMIT = 5          // 회원가입: 5req/분
        const val WINDOW_MS = 60_000L       // 1분 윈도우
        const val CLEANUP_INTERVAL_MS = 300_000L // 5분마다 만료 버킷 정리
    }

    @Volatile
    private var lastCleanup = System.currentTimeMillis()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val clientIp = extractClientIp(request)
        val path = request.requestURI
        val method = request.method

        // 헬스체크는 제한 없음
        if (path == "/api/health") return true

        // 경로별 제한 분류
        val (bucketType, limit) = when {
            path == "/api/members" && method == "POST" -> "signup" to SIGNUP_LIMIT
            path in listOf("/api/vote", "/api/bet") -> "activity" to ACTIVITY_LIMIT
            else -> "general" to GENERAL_LIMIT
        }

        val bucketKey = "$clientIp:$bucketType"
        val now = System.currentTimeMillis()

        // 주기적 정리
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            cleanupExpiredBuckets(now)
            lastCleanup = now
        }

        val bucket = requestCounts.compute(bucketKey) { _, existing ->
            if (existing == null || now - existing.windowStart.get() > WINDOW_MS) {
                // 새 윈도우 시작
                RateLimitBucket(AtomicInteger(1), AtomicLong(now))
            } else {
                existing.count.incrementAndGet()
                existing
            }
        }!!

        val currentCount = bucket.count.get()

        // 제한 초과
        if (currentCount > limit) {
            logger.warn("Rate limit exceeded: IP=$clientIp, bucket=$bucketType, count=$currentCount/$limit")

            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"

            val remaining = ((bucket.windowStart.get() + WINDOW_MS - now) / 1000).coerceAtLeast(1)
            response.setHeader("Retry-After", remaining.toString())
            response.setHeader("X-RateLimit-Limit", limit.toString())
            response.setHeader("X-RateLimit-Remaining", "0")

            val error = ErrorResponse(
                code = "RATE_LIMIT_EXCEEDED",
                message = "요청이 너무 많습니다. ${remaining}초 후 다시 시도해주세요.",
                status = 429
            )
            response.writer.write(objectMapper.writeValueAsString(error))
            return false
        }

        // Rate limit 헤더 항상 추가
        response.setHeader("X-RateLimit-Limit", limit.toString())
        response.setHeader("X-RateLimit-Remaining", (limit - currentCount).coerceAtLeast(0).toString())

        return true
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",").first().trim()
        }
        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }
        return request.remoteAddr ?: "unknown"
    }

    private fun cleanupExpiredBuckets(now: Long) {
        val expired = requestCounts.entries.filter { now - it.value.windowStart.get() > WINDOW_MS * 2 }
        expired.forEach { requestCounts.remove(it.key) }
        if (expired.isNotEmpty()) {
            logger.debug("Cleaned up ${expired.size} expired rate limit buckets")
        }
    }

    data class RateLimitBucket(
        val count: AtomicInteger,
        val windowStart: AtomicLong
    )
}
