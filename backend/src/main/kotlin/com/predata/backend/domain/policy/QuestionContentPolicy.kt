package com.predata.backend.domain.policy

object QuestionContentPolicy {
    private const val TITLE_MIN = 5
    private const val TITLE_MAX = 200
    private const val RULE_MIN = 10
    private const val RULE_MAX = 1000
    private const val DESCRIPTION_MAX = 1000
    private const val TAG_MAX_COUNT = 5
    private const val SOURCE_LINK_MAX_COUNT = 3
    private val BLOCKED_KEYWORDS = setOf("씨발", "병신", "fuck", "shit")

    fun validateTitle(title: String): String? {
        val trimmed = title.trim()
        if (trimmed.length !in TITLE_MIN..TITLE_MAX) {
            return "제목은 ${TITLE_MIN}자 이상 ${TITLE_MAX}자 이하여야 합니다."
        }
        if (trimmed.contains('<') || trimmed.contains('>')) {
            return "제목에 HTML 태그를 포함할 수 없습니다."
        }
        return validateBlockedKeywords(trimmed, "제목")
    }

    fun validateResolutionRule(rule: String?): String? {
        val value = rule?.trim() ?: return "정산 규칙은 필수입니다."
        if (value.length !in RULE_MIN..RULE_MAX) {
            return "정산 규칙은 ${RULE_MIN}자 이상 ${RULE_MAX}자 이하여야 합니다."
        }
        return validateBlockedKeywords(value, "정산 규칙")
    }

    fun validateDescription(description: String?): String? {
        if (description.isNullOrBlank()) return null
        if (description.length > DESCRIPTION_MAX) return "설명은 1000자 이하여야 합니다."
        return null
    }

    fun validateThumbnailUrl(thumbnailUrl: String?): String? {
        if (thumbnailUrl.isNullOrBlank()) return null
        if (!isHttpUrl(thumbnailUrl)) return "thumbnailUrl은 http(s) URL 형식이어야 합니다."
        return null
    }

    fun validateTags(tags: List<String>): String? {
        if (tags.size > TAG_MAX_COUNT) return "태그는 최대 5개까지 가능합니다."
        return null
    }

    fun validateSourceLinks(sourceLinks: List<String>): String? {
        if (sourceLinks.size > SOURCE_LINK_MAX_COUNT) return "소스 링크는 최대 3개까지 가능합니다."
        if (sourceLinks.any { !isHttpUrl(it) }) return "sourceLinks는 모두 http(s) URL 형식이어야 합니다."
        return null
    }

    fun validateCreatorSplitInPool(creatorSplitInPool: Int): String? {
        if (creatorSplitInPool !in 0..100 || creatorSplitInPool % 10 != 0) {
            return "creatorSplitInPool은 0~100 사이 10단위여야 합니다."
        }
        return null
    }

    private fun validateBlockedKeywords(text: String, target: String): String? {
        val lower = text.lowercase()
        if (BLOCKED_KEYWORDS.any { lower.contains(it) }) {
            return "${target}에 사용할 수 없는 표현이 포함되어 있습니다."
        }
        return null
    }

    private fun isHttpUrl(raw: String): Boolean {
        val lower = raw.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }
}
