package com.predata.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val rateLimitInterceptor: RateLimitInterceptor,
    private val banCheckInterceptor: BanCheckInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 1. Rate limiting (모든 API)
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")

        // 2. Ban 체크 (활동 관련 API)
        registry.addInterceptor(banCheckInterceptor)
            .addPathPatterns("/api/vote", "/api/bet", "/api/members")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")  // 모든 경로에 대해 CORS 허용
            .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")  // 모든 로컬호스트 포트 허용
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")  // 모든 HTTP 메서드 허용
            .allowedHeaders("*")  // 모든 헤더 허용
            .exposedHeaders("X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After")  // Rate limit 헤더 노출
            .allowCredentials(true)  // 쿠키 허용
            .maxAge(3600)  // Preflight 캐시 1시간
    }
}
