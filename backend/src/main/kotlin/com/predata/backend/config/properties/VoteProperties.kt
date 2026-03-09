package com.predata.backend.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "app.voting")
@Validated
data class VoteProperties(
    /** 회원당 일일 최대 투표 수 */
    val dailyLimit: Int = 5,

    /**
     * Commit-Reveal 투표 활성화 여부 (Feature Flag)
     * false: POST /api/votes/commit, /api/votes/reveal 요청 시 503 반환
     * true:  VOTE_RESULT 질문에 한해 commit-reveal 엔드포인트 활성화
     */
    val commitRevealEnabled: Boolean = false,
)
