package com.predata.backend.domain.policy

import com.predata.backend.domain.QuestionStatus

object QuestionQuotaPolicy {
    fun shouldBypassQuota(role: String?): Boolean = role?.uppercase() == "ADMIN"

    fun exceedsDailyCreateLimit(dailyCount: Long, dailyLimit: Long = 1L): Boolean =
        dailyCount >= dailyLimit

    fun shouldBlockForActiveQuestion(existsActive: Boolean): Boolean = existsActive

    val ACTIVE_LOCK_STATUSES: Set<QuestionStatus> = setOf(
        QuestionStatus.VOTING,
        QuestionStatus.BREAK,
        QuestionStatus.BETTING,
    )
}
