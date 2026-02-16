package com.predata.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 투표 시스템 설정
 * - dailyLimit: 일일 투표 한도 (UTC 기준)
 */
@Configuration
@ConfigurationProperties(prefix = "app.voting")
data class VotingConfig(
    var dailyLimit: Int = 5
)
