package com.predata.backend.service

import com.predata.backend.dto.LlmGeneratedQuestionDto
import com.predata.backend.dto.QuestionBatchValidationResult
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class QuestionDraftValidator {

    fun validate(
        batchId: String,
        questions: List<LlmGeneratedQuestionDto>,
        requiredCount: Int,
        requiredOpinionCount: Int,
        duplicateThreshold: Double,
        existingTitles: List<String>
    ): DraftValidationOutput {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (questions.size != requiredCount) {
            errors += "질문 개수 불일치: 기대 $requiredCount, 실제 ${questions.size}"
        }

        val normalized = questions.map { normalize(it) }
        val opinionCount = normalized.count { it.marketType == "OPINION" }
        val verifiableCount = normalized.count { it.marketType == "VERIFIABLE" }

        if (opinionCount != requiredOpinionCount) {
            errors += "OPINION 개수 불일치: 기대 $requiredOpinionCount, 실제 $opinionCount"
        }
        if (verifiableCount != (requiredCount - requiredOpinionCount)) {
            errors += "VERIFIABLE 개수 불일치: 기대 ${requiredCount - requiredOpinionCount}, 실제 $verifiableCount"
        }

        val accepted = mutableListOf<ValidatedDraft>()
        val rejected = mutableListOf<Pair<LlmGeneratedQuestionDto, String>>()

        normalized.forEach { q ->
            val duplicateScore = computeDuplicateScore(q.title, existingTitles)
            val reasons = mutableListOf<String>()

            if (q.marketType == "OPINION") {
                if (!q.title.startsWith("시장은 ") || !q.title.endsWith("라고 생각할까요?")) {
                    reasons += "OPINION 제목 템플릿 불일치"
                }
                if (!q.voteResultSettlement) {
                    reasons += "OPINION은 voteResultSettlement=true 이어야 함"
                }
            } else {
                if (q.voteResultSettlement) {
                    reasons += "VERIFIABLE은 voteResultSettlement=false 이어야 함"
                }
                if (q.resolutionSource.isNullOrBlank()) {
                    reasons += "VERIFIABLE은 resolutionSource 필수"
                }
            }

            if (q.resolveAt.isBefore(LocalDateTime.now().plusHours(1))) {
                reasons += "resolveAt이 너무 이릅니다"
            }

            if (duplicateScore >= duplicateThreshold) {
                reasons += "기존 질문과 중복도 높음(score=${"%.2f".format(duplicateScore)})"
            }

            if (reasons.isEmpty()) {
                accepted += ValidatedDraft(q, duplicateScore)
            } else {
                rejected += q to reasons.joinToString("; ")
            }
        }

        val result = QuestionBatchValidationResult(
            batchId = batchId,
            passed = errors.isEmpty() && rejected.isEmpty(),
            total = normalized.size,
            opinionCount = opinionCount,
            verifiableCount = verifiableCount,
            errors = errors,
            warnings = warnings
        )

        return DraftValidationOutput(
            validation = result,
            accepted = accepted,
            rejected = rejected
        )
    }

    private fun normalize(input: LlmGeneratedQuestionDto): LlmGeneratedQuestionDto {
        val marketType = input.marketType.uppercase()
        val questionType = input.questionType.uppercase()
        val normalizedType = if (questionType == "OPINION" || marketType == "OPINION") "OPINION" else "VERIFIABLE"
        val voteResultSettlement = if (normalizedType == "OPINION") true else input.voteResultSettlement

        return input.copy(
            marketType = normalizedType,
            questionType = normalizedType,
            voteResultSettlement = voteResultSettlement
        )
    }

    private fun computeDuplicateScore(title: String, existingTitles: List<String>): Double {
        val normalized = title.trim().lowercase()
        if (normalized.isBlank()) return 1.0

        var maxScore = 0.0
        existingTitles.forEach { existing ->
            val old = existing.trim().lowercase()
            if (old.isBlank()) return@forEach
            if (old == normalized) {
                maxScore = maxOf(maxScore, 1.0)
                return@forEach
            }

            val tokensA = normalized.split(" ").filter { it.isNotBlank() }.toSet()
            val tokensB = old.split(" ").filter { it.isNotBlank() }.toSet()
            val intersection = tokensA.intersect(tokensB).size.toDouble()
            val union = tokensA.union(tokensB).size.toDouble().coerceAtLeast(1.0)
            val jaccard = intersection / union
            maxScore = maxOf(maxScore, jaccard)
        }

        return maxScore
    }
}

data class DraftValidationOutput(
    val validation: QuestionBatchValidationResult,
    val accepted: List<ValidatedDraft>,
    val rejected: List<Pair<LlmGeneratedQuestionDto, String>>
)

data class ValidatedDraft(
    val draft: LlmGeneratedQuestionDto,
    val duplicateScore: Double
)
