package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "app.question-credit")
@Validated
data class QuestionCreditProperties(
    /** 연간 크레딧 예산 */
    val yearlyBudget: Int = 365,

    /** draft 세션 유효 시간 (분) */
    val draftExpireMinutes: Long = 30,

    /** draft-open rate limit (회원 당 분당 최대 횟수) */
    val draftRateLimitPerMinute: Int = 5,

    /** draft-submit rate limit (회원 당 분당 최대 횟수) */
    val submitRateLimitPerMinute: Int = 3,
)
