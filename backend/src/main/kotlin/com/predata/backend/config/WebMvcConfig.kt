package com.predata.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val rateLimitInterceptor: RateLimitInterceptor,
    private val banCheckInterceptor: BanCheckInterceptor,
    private val jwtAuthInterceptor: JwtAuthInterceptor,
    private val adminAuthInterceptor: AdminAuthInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 1. Rate limiting (모든 API)
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")

        // 2. JWT 인증 (화이트리스트 방식: 공개 API만 명시적으로 제외, 나머지는 모두 인증 필요)
        registry.addInterceptor(jwtAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                // 인증/회원가입
                "/api/auth/**",
                "/api/members",              // POST: 회원가입만 공개

                // 공개 조회 API
                "/api/health",
                // /api/questions/** - GET만 JwtAuthInterceptor에서 제외 처리
                "/api/leaderboard/**",       // 리더보드
                "/api/analytics/**",         // 분석 데이터
                "/api/blockchain/**",        // 블록체인 데이터
                "/api/badges/**",            // 배지 정보
                "/api/tiers/**",             // 티어 정보
                "/api/betting/suspension/**", // 베팅 정지 정보

                // 공개 회원 조회 (ID/지갑 기반만)
                // /api/members/{id} - JwtAuthInterceptor에서 숫자 ID만 제외 처리
                "/api/members/by-wallet"     // GET /members/by-wallet - 지갑 주소로 조회

                // ❌ /api/members/by-email 제거 (보안상 위험)
                // ❌ /api/payments/** 제거 (인증 필수)
                // ❌ /api/members/* 제거 (/api/members/me는 인증 필수)
                // ✅ /api/members/me 인증 필요 (JWT 인터셉터가 처리)
            )

        // 3. Admin 권한 체크 (관리자 경로만)
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**")

        // 4. Ban 체크 (활동 관련 API)
        registry.addInterceptor(banCheckInterceptor)
            .addPathPatterns("/api/vote", "/api/bet", "/api/members")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")  // 모든 경로에 대해 CORS 허용
            .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "https://predata.vercel.app", "https://*.vercel.app", "https://*.trycloudflare.com", "https://api.predata.io", "https://predata.io", "https://www.predata.io")  // 모든 로컬호스트 포트 허용
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")  // 모든 HTTP 메서드 허용
            .allowedHeaders("*")  // 모든 헤더 허용
            .exposedHeaders("X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After")  // Rate limit 헤더 노출
            .allowCredentials(true)  // 쿠키 허용
            .maxAge(3600)  // Preflight 캐시 1시간
    }
}
