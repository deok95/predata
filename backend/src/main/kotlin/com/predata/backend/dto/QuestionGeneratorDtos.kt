package com.predata.backend.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

// ===== Settings related DTOs =====

data class QuestionGeneratorSettingsResponse(
    val enabled: Boolean,
    val intervalSeconds: Long,
    val categories: List<String>,
    val region: String = "US",
    val dailyCount: Int = 3,
    val opinionCount: Int = 1,
    val votingHours: Int = 24,
    val bettingHours: Int = 24,
    val breakMinutes: Int = 30,
    val revealMinutes: Int = 30,
    val lastGeneratedAt: String? = null,
    val isDemoMode: Boolean = false
)

data class UpdateQuestionGeneratorSettingsRequest(
    val enabled: Boolean? = null,

    @field:Min(value = 60, message = "Minimum interval is 60 seconds.")
    val intervalSeconds: Long? = null,

    val categories: List<String>? = null,

    val region: String? = null,

    @field:Min(value = 1, message = "Daily generation count must be at least 1.")
    val dailyCount: Int? = null,

    @field:Min(value = 1, message = "Opinion generation count must be at least 1.")
    val opinionCount: Int? = null,

    @field:Min(value = 1, message = "Voting time must be at least 1 hour.")
    val votingHours: Int? = null,

    @field:Min(value = 1, message = "Betting time must be at least 1 hour.")
    val bettingHours: Int? = null,

    @field:Min(value = 0, message = "Break time must be 0 minutes or more.")
    val breakMinutes: Int? = null,

    @field:Min(value = 1, message = "Reveal time must be at least 1 minute.")
    val revealMinutes: Int? = null
)

// ===== Question generation related DTOs =====

data class ManualGenerateQuestionRequest(
    val category: String? = null
)

data class QuestionGenerationResponse(
    val success: Boolean,
    val questionId: Long? = null,
    val title: String? = null,
    val category: String? = null,
    val message: String,
    val isDemoMode: Boolean = false
)

// ===== Claude API internal DTOs =====

data class ClaudeApiRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)
