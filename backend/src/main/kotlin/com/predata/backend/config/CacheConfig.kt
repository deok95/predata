package com.predata.backend.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * 캐시 설정
 * - 데이터센터 API 성능 최적화를 위한 캐싱 적용
 * - TTL: 60초 (투표/베팅 데이터는 자주 변경될 수 있으므로 짧게 설정)
 */
@Configuration
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)  // 60초 TTL
                .maximumSize(500)                         // 최대 500개 캐시 항목
                .recordStats()                            // 통계 기록
        )
        // 사용할 캐시 이름 등록
        cacheManager.setCacheNames(
            listOf(
                "weightedVotes",      // PersonaWeightService
                "votesByCountry",     // PersonaWeightService
                "abusingReport",      // AbusingDetectionService
                "qualityScore",       // DataQualityService
                "demographics",       // AnalyticsService
                "qualityDashboard"    // AnalyticsService
            )
        )
        return cacheManager
    }
}
