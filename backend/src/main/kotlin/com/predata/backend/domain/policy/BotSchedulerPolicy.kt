package com.predata.backend.domain.policy

object BotSchedulerPolicy {
    fun shouldRun(botEnabled: Boolean): Boolean = botEnabled
}
