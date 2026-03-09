package com.predata.backend.domain.policy

import java.time.LocalDateTime

data class Top3CandidateInput(
    val questionId: Long,
    val voteCount: Long,
    val createdAt: LocalDateTime,
    val normalizedHash: String?,
)

data class RankedTop3Candidate(
    val questionId: Long,
    val rankInCategory: Int,
    val voteCount: Long,
    val canonicalQuestionId: Long?,
    val selectedTop3: Boolean,
    val duplicate: Boolean,
)

object Top3SelectionPolicy {
    private const val FIXED_TOP3_LIMIT = 3

    fun rankAndSelect(
        inputs: List<Top3CandidateInput>,
        minOpenPerCategory: Int,
    ): List<RankedTop3Candidate> {
        val sorted = inputs.sortedWith(
            compareByDescending<Top3CandidateInput> { it.voteCount }
                .thenBy { it.createdAt }
                .thenBy { it.questionId }
        )
        val sortedIds = sorted.map { it.questionId }
        val hashMap = sorted.associate { it.questionId to it.normalizedHash }
        val canonicalByQuestionId = CanonicalDuplicatePolicy.canonicalQuestionIdsByHash(sortedIds, hashMap)

        val eligibleIds = sortedIds.filter { questionId ->
            !CanonicalDuplicatePolicy.isDuplicate(questionId, canonicalByQuestionId[questionId])
        }
        val selectCount = if (eligibleIds.size >= minOpenPerCategory) FIXED_TOP3_LIMIT else 0
        val selected = eligibleIds.take(selectCount).toSet()

        return sorted.mapIndexed { index, row ->
            val canonicalId = canonicalByQuestionId[row.questionId]
            val duplicate = CanonicalDuplicatePolicy.isDuplicate(row.questionId, canonicalId)
            RankedTop3Candidate(
                questionId = row.questionId,
                rankInCategory = index + 1,
                voteCount = row.voteCount,
                canonicalQuestionId = canonicalId,
                selectedTop3 = !duplicate && row.questionId in selected,
                duplicate = duplicate,
            )
        }
    }
}
