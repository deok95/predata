package com.predata.backend.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

// ===== 설정 관련 DTOs =====

data class QuestionGeneratorSettingsResponse(
    val enabled: Boolean,
    val intervalSeconds: Long,
    val categories: List<String>,
    val lastGeneratedAt: String? = null,
    val isDemoMode: Boolean = false
)

data class UpdateQuestionGeneratorSettingsRequest(
    val enabled: Boolean? = null,

    @field:Min(value = 60, message = "최소 간격은 60초입니다.")
    val intervalSeconds: Long? = null,

    val categories: List<String>? = null
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
