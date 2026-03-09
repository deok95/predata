package com.predata.backend

import com.predata.backend.domain.policy.QuestionContentPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestionContentPolicyTest {
    @Test
    fun `validateTitle blocks html and bad keywords`() {
        assertEquals("제목에 HTML 태그를 포함할 수 없습니다.", QuestionContentPolicy.validateTitle("<b>hello</b>"))
        val blocked = QuestionContentPolicy.validateTitle("this is shit")
        kotlin.test.assertTrue(blocked!!.contains("사용할 수 없는 표현"))
    }

    @Test
    fun `validateSourceLinks validates max and scheme`() {
        assertEquals(
            "소스 링크는 최대 3개까지 가능합니다.",
            QuestionContentPolicy.validateSourceLinks(listOf("https://a", "https://b", "https://c", "https://d"))
        )
        assertEquals(
            "sourceLinks는 모두 http(s) URL 형식이어야 합니다.",
            QuestionContentPolicy.validateSourceLinks(listOf("ftp://a"))
        )
        assertNull(QuestionContentPolicy.validateSourceLinks(listOf("https://ok")))
    }
}
