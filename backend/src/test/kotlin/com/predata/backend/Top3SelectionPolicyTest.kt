package com.predata.backend

import com.predata.backend.domain.policy.Top3CandidateInput
import com.predata.backend.domain.policy.Top3SelectionPolicy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Top3SelectionPolicyTest {
    @Test
    fun `rankAndSelect applies vote createdAt questionId sort and selects top3`() {
        val now = LocalDateTime.now()
        val inputs = listOf(
            Top3CandidateInput(questionId = 2L, voteCount = 10, createdAt = now.minusHours(1), normalizedHash = "A"),
            Top3CandidateInput(questionId = 1L, voteCount = 10, createdAt = now.minusHours(1), normalizedHash = "A"),
            Top3CandidateInput(questionId = 3L, voteCount = 8, createdAt = now.minusHours(2), normalizedHash = "B"),
            Top3CandidateInput(questionId = 4L, voteCount = 7, createdAt = now.minusHours(3), normalizedHash = "C"),
        )

        val ranked = Top3SelectionPolicy.rankAndSelect(inputs, minOpenPerCategory = 1)
        val byId = ranked.associateBy { it.questionId }

        assertEquals(1, byId[1L]!!.rankInCategory)
        assertTrue(byId[1L]!!.selectedTop3)
        assertTrue(byId[2L]!!.duplicate)
        assertFalse(byId[2L]!!.selectedTop3)
        assertTrue(byId[3L]!!.selectedTop3)
        assertTrue(byId[4L]!!.selectedTop3)
    }

    @Test
    fun `rankAndSelect selects none when eligible count is below min open threshold`() {
        val now = LocalDateTime.now()
        val inputs = listOf(
            Top3CandidateInput(questionId = 1L, voteCount = 5, createdAt = now, normalizedHash = null),
            Top3CandidateInput(questionId = 2L, voteCount = 4, createdAt = now.plusMinutes(1), normalizedHash = null),
        )

        val ranked = Top3SelectionPolicy.rankAndSelect(inputs, minOpenPerCategory = 3)
        assertEquals(0, ranked.count { it.selectedTop3 })
    }
}
