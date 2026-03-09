package com.predata.backend.domain.policy

object SocialPolicy {
    private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,30}$")

    fun normalizeUsername(username: String?): String? {
        val value = username?.trim()?.lowercase()
        return if (value.isNullOrBlank()) null else value
    }

    fun isValidUsername(username: String): Boolean = USERNAME_REGEX.matches(username)

    fun canFollow(followerId: Long, targetMemberId: Long): Boolean = followerId != targetMemberId

    fun normalizePage(page: Int): Int = page.coerceAtLeast(0)

    fun normalizePageSize(size: Int, min: Int = 1, max: Int = 100): Int = size.coerceIn(min, max)

    fun normalizeCommentContent(content: String): String = content.trim()

    fun isCommentContentValid(content: String, maxLength: Int = 1000): Boolean =
        content.isNotBlank() && content.length <= maxLength
}
