package com.predata.backend

import com.predata.backend.domain.policy.CanonicalDuplicatePolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanonicalDuplicatePolicyTest {
    @Test
    fun `canonicalQuestionIdsByHash marks first question as canonical`() {
        val sortedIds = listOf(10L, 11L, 12L, 13L)
        val hashes = mapOf(
            10L to "A",
            11L to "A",
            12L to "B",
            13L to null,
        )

        val canonical = CanonicalDuplicatePolicy.canonicalQuestionIdsByHash(sortedIds, hashes)

        assertEquals(10L, canonical[10L])
        assertEquals(10L, canonical[11L])
        assertEquals(12L, canonical[12L])
        assertEquals(null, canonical[13L])
        assertFalse(CanonicalDuplicatePolicy.isDuplicate(10L, canonical[10L]))
        assertTrue(CanonicalDuplicatePolicy.isDuplicate(11L, canonical[11L]))
    }
}
