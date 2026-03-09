package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * 온체인 릴레이 설정
 *
 * app.relay.enabled   – false 면 스케줄러 비활성
 * app.relay.mode      – mock(기본) | polygon
 * app.relay.batch-size – 스케줄러 1회 처리 최대 건수
 * app.relay.max-retry  – 최대 재시도 횟수 (초과 시 FAILED_FINAL)
 */
@ConfigurationProperties(prefix = "app.relay")
@Validated
data class RelayProperties(
    val enabled: Boolean = true,
    val mode: String = "mock",
    val batchSize: Int = 50,
    val maxRetry: Int = 8,
)
