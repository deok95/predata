package com.predata.backend.service

import com.predata.backend.domain.Follow
import com.predata.backend.domain.QuestionComment
import com.predata.backend.domain.policy.SocialPolicy
import com.predata.backend.dto.*
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.ForbiddenException
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.FollowRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionCommentRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SocialService(
    private val memberRepository: MemberRepository,
    private val followRepository: FollowRepository,
    private val questionRepository: QuestionRepository,
    private val questionCommentRepository: QuestionCommentRepository,
) {
    @Transactional(readOnly = true)
    fun getProfile(targetMemberId: Long, requesterId: Long?): SocialProfileResponse {
        val target = memberRepository.findById(targetMemberId)
            .orElseThrow { NotFoundException("Member not found.") }

        val followers = followRepository.countByFollowingId(targetMemberId)
        val following = followRepository.countByFollowerId(targetMemberId)
        val isFollowing = if (requesterId != null && requesterId != targetMemberId) {
            followRepository.existsByFollowerIdAndFollowingId(requesterId, targetMemberId)
        } else {
            false
        }

        return SocialProfileResponse(
            memberId = target.id!!,
            username = target.username,
            displayName = target.displayName,
            bio = target.bio,
            avatarUrl = target.avatarUrl,
            followersCount = followers,
            followingCount = following,
            isFollowing = isFollowing,
        )
    }

    @Transactional
    fun updateMyProfile(memberId: Long, request: UpdateSocialProfileRequest): SocialProfileResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("Member not found.") }

        val normalizedUsername = SocialPolicy.normalizeUsername(request.username)
        if (!normalizedUsername.isNullOrBlank()) {
            if (!SocialPolicy.isValidUsername(normalizedUsername)) {
                throw ConflictException("Username must be 3-30 chars and contain only lowercase letters, numbers, or underscore.")
            }
            if (memberRepository.existsByUsernameAndIdNot(normalizedUsername, memberId)) {
                throw ConflictException("Username already in use.")
            }
            member.username = normalizedUsername
        }

        request.displayName?.let { member.displayName = it.trim() }
        request.bio?.let { member.bio = it.trim() }
        request.avatarUrl?.let { member.avatarUrl = it.trim() }
        memberRepository.save(member)

        return getProfile(memberId, memberId)
    }

    @Transactional
    fun follow(followerId: Long, targetMemberId: Long): FollowActionResponse {
        if (!SocialPolicy.canFollow(followerId, targetMemberId)) {
            throw ConflictException("You cannot follow yourself.")
        }
        memberRepository.findById(targetMemberId)
            .orElseThrow { NotFoundException("Member not found.") }

        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, targetMemberId)) {
            followRepository.save(Follow(followerId = followerId, followingId = targetMemberId))
        }

        return FollowActionResponse(targetMemberId = targetMemberId, following = true)
    }

    @Transactional
    fun unfollow(followerId: Long, targetMemberId: Long): FollowActionResponse {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, targetMemberId)
        return FollowActionResponse(targetMemberId = targetMemberId, following = false)
    }

    @Transactional(readOnly = true)
    fun getFollowers(targetMemberId: Long, page: Int, size: Int): FollowListResponse {
        memberRepository.findById(targetMemberId)
            .orElseThrow { NotFoundException("Member not found.") }
        val pageable = PageRequest.of(SocialPolicy.normalizePage(page), SocialPolicy.normalizePageSize(size))
        val follows = followRepository.findByFollowingIdOrderByCreatedAtDesc(targetMemberId, pageable)
        return buildFollowListResponse(follows.content.map { it.followerId to it.createdAt }, follows.totalElements, follows.totalPages, follows.number, follows.size)
    }

    @Transactional(readOnly = true)
    fun getFollowing(targetMemberId: Long, page: Int, size: Int): FollowListResponse {
        memberRepository.findById(targetMemberId)
            .orElseThrow { NotFoundException("Member not found.") }
        val pageable = PageRequest.of(SocialPolicy.normalizePage(page), SocialPolicy.normalizePageSize(size))
        val follows = followRepository.findByFollowerIdOrderByCreatedAtDesc(targetMemberId, pageable)
        return buildFollowListResponse(follows.content.map { it.followingId to it.createdAt }, follows.totalElements, follows.totalPages, follows.number, follows.size)
    }

    @Transactional(readOnly = true)
    fun getQuestionComments(questionId: Long, requesterId: Long?, page: Int, size: Int): CommentListResponse {
        questionRepository.findById(questionId)
            .orElseThrow { NotFoundException("Question not found.") }
        val pageable = PageRequest.of(SocialPolicy.normalizePage(page), SocialPolicy.normalizePageSize(size))
        val commentsPage = questionCommentRepository.findByQuestionIdOrderByCreatedAtDesc(questionId, pageable)
        val memberMap = memberRepository.findAllByIdIn(commentsPage.content.map { it.memberId }.distinct()).associateBy { it.id!! }

        val items = commentsPage.content.map { comment ->
            val member = memberMap[comment.memberId]
            QuestionCommentResponse(
                commentId = comment.id!!,
                questionId = comment.questionId,
                memberId = comment.memberId,
                username = member?.username,
                displayName = member?.displayName,
                avatarUrl = member?.avatarUrl,
                parentCommentId = comment.parentCommentId,
                content = comment.content,
                likeCount = comment.likeCount,
                isMine = requesterId != null && requesterId == comment.memberId,
                createdAt = comment.createdAt.toString(),
                updatedAt = comment.updatedAt.toString(),
            )
        }

        return CommentListResponse(
            items = items,
            totalElements = commentsPage.totalElements,
            totalPages = commentsPage.totalPages,
            page = commentsPage.number,
            size = commentsPage.size,
        )
    }

    @Transactional
    fun createQuestionComment(questionId: Long, memberId: Long, request: CreateCommentRequest): QuestionCommentResponse {
        questionRepository.findById(questionId)
            .orElseThrow { NotFoundException("Question not found.") }
        memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("Member not found.") }

        request.parentCommentId?.let { parentId ->
            val parent = questionCommentRepository.findById(parentId)
                .orElseThrow { NotFoundException("Parent comment not found.") }
            if (parent.questionId != questionId) {
                throw ConflictException("Parent comment does not belong to this question.")
            }
        }

        val normalizedContent = SocialPolicy.normalizeCommentContent(request.content)
        if (!SocialPolicy.isCommentContentValid(normalizedContent)) {
            throw ConflictException("Comment content is required.")
        }

        val now = LocalDateTime.now()
        val saved = questionCommentRepository.save(
            QuestionComment(
                questionId = questionId,
                memberId = memberId,
                parentCommentId = request.parentCommentId,
                content = normalizedContent,
                createdAt = now,
                updatedAt = now,
            )
        )

        val member = memberRepository.findById(memberId).orElseThrow { NotFoundException("Member not found.") }
        return QuestionCommentResponse(
            commentId = saved.id!!,
            questionId = saved.questionId,
            memberId = saved.memberId,
            username = member.username,
            displayName = member.displayName,
            avatarUrl = member.avatarUrl,
            parentCommentId = saved.parentCommentId,
            content = saved.content,
            likeCount = saved.likeCount,
            isMine = true,
            createdAt = saved.createdAt.toString(),
            updatedAt = saved.updatedAt.toString(),
        )
    }

    @Transactional
    fun deleteComment(questionId: Long, commentId: Long, requesterId: Long) {
        val comment = questionCommentRepository.findById(commentId)
            .orElseThrow { NotFoundException("Comment not found.") }
        if (comment.questionId != questionId) {
            throw NotFoundException("Comment not found.")
        }
        if (comment.memberId != requesterId) {
            throw ForbiddenException("You can only delete your own comments.")
        }
        questionCommentRepository.delete(comment)
    }

    private fun buildFollowListResponse(
        idsWithTime: List<Pair<Long, LocalDateTime>>,
        total: Long,
        totalPages: Int,
        page: Int,
        size: Int,
    ): FollowListResponse {
        val memberIds = idsWithTime.map { it.first }.distinct()
        val memberMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }

        val items = idsWithTime.mapNotNull { (memberId, followedAt) ->
            val member = memberMap[memberId] ?: return@mapNotNull null
            FollowUserItem(
                memberId = memberId,
                username = member.username,
                displayName = member.displayName,
                avatarUrl = member.avatarUrl,
                followedAt = followedAt.toString(),
            )
        }

        return FollowListResponse(
            items = items,
            totalElements = total,
            totalPages = totalPages,
            page = page,
            size = size,
        )
    }
}
