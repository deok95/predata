package com.predata.backend.service

import com.predata.backend.config.properties.SystemProperties
import com.predata.backend.domain.VoteWindowType
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.FollowRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionCommentRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class VoteFeedService(
    private val activityRepository: ActivityRepository,
    private val followRepository: FollowRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val questionCommentRepository: QuestionCommentRepository,
    private val systemProperties: SystemProperties,
) {
    enum class FeedMode { FORYOU, FOLLOWING, TOP10 }
    enum class Window { H3, H6, D1, D3 }

    data class VoteFeedItem(
        val questionId: Long,
        val title: String,
        val category: String?,
        val yesVotes: Long,
        val noVotes: Long,
        val totalVotes: Long,
        val yesPrice: Double,
        val commentCount: Long,
        val submitterId: Long?,
        val submitterUsername: String?,
        val submitterDisplayName: String?,
        val submitterAvatarUrl: String?,
        val isFollowingSubmitter: Boolean,
        val submittedAt: String,
        val lastVoteAt: String,
        val votingEndAt: String,
    )

    @Transactional(readOnly = true)
    fun getFeed(
        mode: FeedMode,
        requesterId: Long?,
        category: String?,
        window: Window,
        voteWindowType: String?,
        limit: Int,
    ): List<VoteFeedItem> {
        val safeLimit = limit.coerceIn(1, 50)
        val windowTypeFilter = voteWindowType?.uppercase()?.let {
            runCatching { VoteWindowType.valueOf(it) }.getOrNull()
        }
        val candidates = activityRepository.findTopVotedQuestionsSince(
            since = since(window),
            category = category?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            voteWindowType = windowTypeFilter,
            pageable = PageRequest.of(0, 200),
        )
        if (candidates.isEmpty()) return emptyList()

        val questionIds = candidates.map { it.getQuestionId() }.distinct()
        val questionsById = questionRepository.findAllById(questionIds).associateBy { it.id!! }
        // creatorMemberId가 null인 어드민 생성 질문은 predata 공식 계정으로 귀속
        val adminId = systemProperties.predataAdminMemberId
        val creatorIds = (questionsById.values.mapNotNull { it.creatorMemberId } + adminId).distinct()
        val creatorsById = if (creatorIds.isNotEmpty()) {
            memberRepository.findAllByIdIn(creatorIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        val followingIds = if (requesterId != null) {
            followRepository.findFollowingIdsByFollowerId(requesterId).toSet()
        } else {
            emptySet()
        }

        // Batch-fetch comment counts to avoid N+1 queries
        val commentCountById = questionCommentRepository.countsByQuestionIds(questionIds)
            .associate { it.getQuestionId() to it.getCount() }

        val mapped = candidates.mapNotNull { row ->
            val question = questionsById[row.getQuestionId()] ?: return@mapNotNull null
            // 어드민 생성 질문(creatorMemberId == null) → predata 공식 계정으로 귀속
            val resolvedCreatorId = question.creatorMemberId ?: adminId
            val creator = creatorsById[resolvedCreatorId]
            val total = row.getTotalVotes().coerceAtLeast(1)
            VoteFeedItem(
                questionId = row.getQuestionId(),
                title = row.getTitle(),
                category = row.getCategory(),
                yesVotes = row.getYesVotes(),
                noVotes = row.getNoVotes(),
                totalVotes = row.getTotalVotes(),
                yesPrice = row.getYesVotes().toDouble() / total.toDouble(),
                commentCount = commentCountById[row.getQuestionId()] ?: 0L,
                submitterId = resolvedCreatorId,
                submitterUsername = creator?.username,
                submitterDisplayName = creator?.displayName,
                submitterAvatarUrl = creator?.avatarUrl,
                isFollowingSubmitter = followingIds.contains(resolvedCreatorId),
                submittedAt = question.createdAt.toString(),
                lastVoteAt = row.getLastVoteAt().toString(),
                votingEndAt = question.votingEndAt.toString(),
            )
        }

        val ranked = when (mode) {
            FeedMode.TOP10 -> mapped.sortedWith(compareByDescending<VoteFeedItem> { it.totalVotes }.thenByDescending { it.lastVoteAt })
            FeedMode.FOLLOWING -> {
                if (requesterId == null) emptyList()
                else mapped.filter { it.submitterId != null && followingIds.contains(it.submitterId) }
                    .sortedWith(compareByDescending<VoteFeedItem> { it.lastVoteAt }.thenByDescending { it.totalVotes })
            }
            FeedMode.FORYOU -> {
                if (requesterId == null) {
                    mapped.sortedWith(compareByDescending<VoteFeedItem> { it.totalVotes }.thenByDescending { it.lastVoteAt })
                } else {
                    mapped.sortedWith(
                        compareByDescending<VoteFeedItem> { it.isFollowingSubmitter }
                            .thenByDescending { it.totalVotes }
                            .thenByDescending { it.lastVoteAt }
                    )
                }
            }
        }

        return ranked.take(safeLimit)
    }

    private fun since(window: Window): LocalDateTime {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return when (window) {
            Window.H3 -> now.minusHours(3)
            Window.H6 -> now.minusHours(6)
            Window.D1 -> now.minusDays(1)
            Window.D3 -> now.minusDays(3)
        }
    }
}
