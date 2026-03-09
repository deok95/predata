package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal

/**
 * 마켓 배치 오픈 설정
 *
 * prefix: app.market-batch
 */
@ConfigurationProperties(prefix = "app.market-batch")
@Validated
data class MarketBatchProperties(
    /** 스케줄러 활성화 여부 */
    val enabled: Boolean = true,

    /** Spring cron 표현식 (기본: 5분 주기) */
    val cronExpression: String = "0 */5 * * * *",

    /** 카테고리별 최대 선별 수 */
    val top3PerCategory: Int = 3,

    /** 카테고리별 최소 오픈 기준 (미만이면 해당 카테고리 스킵하지 않음 — 있는 만큼 오픈) */
    val minOpenPerCategory: Int = 1,

    /** AMM 풀 시드 USDC 금액 */
    val seedUsdc: BigDecimal = BigDecimal("500"),

    /** AMM 풀 수수료율 */
    val feeRate: BigDecimal = BigDecimal("0.01"),
) {
    init {
        require(top3PerCategory == 3) {
            "app.market-batch.top3-per-category must be fixed to 3"
        }
    }
}
