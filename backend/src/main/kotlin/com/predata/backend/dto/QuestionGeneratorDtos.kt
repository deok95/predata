package com.predata.backend.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

// ===== 설정 관련 DTOs =====

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

    @field:Min(value = 60, message = "최소 간격은 60초입니다.")
    val intervalSeconds: Long? = null,

    val categories: List<String>? = null,

    val region: String? = null,

    @field:Min(value = 1, message = "하루 생성 수는 최소 1이어야 합니다.")
    val dailyCount: Int? = null,

    @field:Min(value = 1, message = "의견형 생성 수는 최소 1이어야 합니다.")
    val opinionCount: Int? = null,

    @field:Min(value = 1, message = "투표 시간은 최소 1시간이어야 합니다.")
    val votingHours: Int? = null,

    @field:Min(value = 1, message = "베팅 시간은 최소 1시간이어야 합니다.")
    val bettingHours: Int? = null,

    @field:Min(value = 0, message = "브레이크 시간은 0분 이상이어야 합니다.")
    val breakMinutes: Int? = null,

    @field:Min(value = 1, message = "리빌 시간은 최소 1분이어야 합니다.")
    val revealMinutes: Int? = null
)

// ===== 질문 생성 관련 DTOs =====

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

// ===== Claude API 내부용 DTOs =====

data class ClaudeApiRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)
