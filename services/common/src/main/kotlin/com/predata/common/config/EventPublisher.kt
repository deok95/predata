package com.predata.common.config

import com.predata.common.events.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class EventPublisher(private val redisTemplate: RedisTemplate<String, Any>) {

    fun publishBetPlacedEvent(event: BetPlacedEvent) {
        redisTemplate.convertAndSend("events:bet-placed", event)
    }

    fun publishSettlementCompletedEvent(event: SettlementCompletedEvent) {
        redisTemplate.convertAndSend("events:settlement-completed", event)
    }

    fun publishQuestionSettledEvent(event: QuestionSettledEvent) {
        redisTemplate.convertAndSend("events:question-settled", event)
    }

    fun publishMatchUpdatedEvent(event: MatchUpdatedEvent) {
        redisTemplate.convertAndSend("events:match-updated", event)
    }

    fun publishQuestionCreatedEvent(event: QuestionCreatedEvent) {
        redisTemplate.convertAndSend("events:question-created", event)
    }
}
