package com.predata.backend.domain.policy

object CanonicalDuplicatePolicy {
    fun canonicalQuestionIdsByHash(
        sortedQuestionIds: List<Long>,
        normalizedHashByQuestionId: Map<Long, String?>,
    ): Map<Long, Long?> {
        val canonicalByHash = mutableMapOf<String, Long>()
        val canonicalByQuestionId = mutableMapOf<Long, Long?>()

        sortedQuestionIds.forEach { questionId ->
            val hash = normalizedHashByQuestionId[questionId]
            if (hash.isNullOrBlank()) {
                canonicalByQuestionId[questionId] = null
                return@forEach
            }
            val canonical = canonicalByHash.getOrPut(hash) { questionId }
            canonicalByQuestionId[questionId] = canonical
        }

        return canonicalByQuestionId
    }

    fun isDuplicate(questionId: Long, canonicalQuestionId: Long?): Boolean =
        canonicalQuestionId != null && canonicalQuestionId != questionId
}
