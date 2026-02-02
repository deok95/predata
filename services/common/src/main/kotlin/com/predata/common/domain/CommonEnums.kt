package com.predata.common.domain

// Activity Type
enum class ActivityType {
    VOTE,
    BET
}

// Choice
enum class Choice {
    YES,
    NO
}

// Final Result
enum class FinalResult {
    YES,
    NO,
    PENDING
}

// Question Status
enum class QuestionStatus {
    OPEN,
    CLOSED,
    SETTLED
}

// Tier
enum class Tier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}
