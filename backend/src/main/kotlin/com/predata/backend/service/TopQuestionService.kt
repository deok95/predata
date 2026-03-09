package com.predata.backend.service

import com.predata.backend.repository.ActivityRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class TopQuestionService(
    private val activityRepository: ActivityRepository,
) {
    enum class VoteWindow {
        H6, D1, D3
    }

    data class TopQuestionItem(
        val questionId: Long,
        val title: String,
        val category: String?,
        val yesVotes: Long,
        val noVotes: Long,
        val totalVotes: Long,
        val yesPrice: Double,
        val lastVoteAt: String,
    )

    @Transactional(readOnly = true)
    fun getTopQuestions(
        category: String?,
        window: VoteWindow,
        limit: Int,
    ): List<TopQuestionItem> {
        val since = resolveSince(window)
        val safeLimit = limit.coerceIn(1, 20)
        val rows = activityRepository.findTopVotedQuestionsSince(
            since = since,
            category = category?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            voteWindowType = null,
            pageable = PageRequest.of(0, safeLimit),
        )

        return rows.map { row ->
            val yes = row.getYesVotes()
            val total = row.getTotalVotes().coerceAtLeast(1)
            TopQuestionItem(
                questionId = row.getQuestionId(),
                title = row.getTitle(),
                category = row.getCategory(),
                yesVotes = yes,
                noVotes = row.getNoVotes(),
                totalVotes = row.getTotalVotes(),
                yesPrice = yes.toDouble() / total.toDouble(),
                lastVoteAt = row.getLastVoteAt().toString(),
            )
        }
    }

    private fun resolveSince(window: VoteWindow): LocalDateTime {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return when (window) {
            VoteWindow.H6 -> now.minusHours(6)
            VoteWindow.D1 -> now.minusDays(1)
            VoteWindow.D3 -> now.minusDays(3)
        }
    }
}
