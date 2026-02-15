package com.predata.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

/**
 * 리워드 시스템 설정
 * - 레벨별 가중치
 * - 최소 지급 금액
 */
@Configuration
@ConfigurationProperties(prefix = "app.reward")
data class RewardConfig(
    /**
     * 레벨별 가중치 (1~5)
     */
    var levelWeights: Map<Int, BigDecimal> = mapOf(
        1 to BigDecimal("1.0"),
        2 to BigDecimal("1.2"),
        3 to BigDecimal("1.5"),
        4 to BigDecimal("2.0"),
        5 to BigDecimal("3.0")
    ),

    /**
     * 최소 지급 금액 (0.01 미만 절사)
     */
    var minPayout: BigDecimal = BigDecimal("0.01")
) {
    /**
     * 레벨에 해당하는 가중치 조회
     */
    fun getWeight(level: Int): BigDecimal {
        return levelWeights[level] ?: BigDecimal.ONE
    }
}
