package com.predata.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 리스크 관리 설정
 */
@Configuration
@ConfigurationProperties(prefix = "app.risk")
data class RiskConfig(
    /**
     * 시장당 최대 포지션 크기 (단위: quantity)
     */
    var maxPositionPerMarket: Int = 1000,

    /**
     * 단일 주문 최대 금액 (단위: USDC)
     */
    var maxOrderValue: Int = 500,

    /**
     * 서킷 브레이커 설정
     */
    var circuitBreaker: CircuitBreakerConfig = CircuitBreakerConfig()
) {
    data class CircuitBreakerConfig(
        /**
         * 트리거되는 거래 횟수
         */
        var tradeCountThreshold: Int = 100,

        /**
         * 시간 윈도우 (초)
         */
        var timeWindowSeconds: Int = 60,

        /**
         * 쿨다운 기간 (초)
         */
        var cooldownSeconds: Int = 30
    )
}
