package com.predata.backend

import com.predata.backend.domain.policy.SocialPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SocialPolicyTest {
    @Test
    fun `normalizeUsername lowercases and trims`() {
        assertEquals("alpha_user", SocialPolicy.normalizeUsername("  Alpha_User "))
        assertNull(SocialPolicy.normalizeUsername("   "))
    }

    @Test
    fun `isValidUsername enforces format`() {
        assertTrue(SocialPolicy.isValidUsername("user_123"))
        assertFalse(SocialPolicy.isValidUsername("User_123"))
        assertFalse(SocialPolicy.isValidUsername("ab"))
    }

    @Test
    fun `page and size normalization clamps values`() {
        assertEquals(0, SocialPolicy.normalizePage(-1))
        assertEquals(1, SocialPolicy.normalizePageSize(0))
        assertEquals(100, SocialPolicy.normalizePageSize(999))
    }

    @Test
    fun `comment content validation requires non blank`() {
        val normalized = SocialPolicy.normalizeCommentContent("  hi  ")
        assertEquals("hi", normalized)
        assertTrue(SocialPolicy.isCommentContentValid(normalized))
        assertFalse(SocialPolicy.isCommentContentValid("   "))
    }
}
