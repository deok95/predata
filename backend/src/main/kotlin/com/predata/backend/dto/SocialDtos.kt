package com.predata.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SocialProfileResponse(
    val memberId: Long,
    val username: String?,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val followersCount: Long,
    val followingCount: Long,
    val isFollowing: Boolean,
)

data class FollowUserItem(
    val memberId: Long,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val followedAt: String,
)

data class FollowListResponse(
    val items: List<FollowUserItem>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

data class FollowActionResponse(
    val targetMemberId: Long,
    val following: Boolean,
)

data class QuestionCommentResponse(
    val commentId: Long,
    val questionId: Long,
    val memberId: Long,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val parentCommentId: Long?,
    val content: String,
    val likeCount: Int,
    val isMine: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

data class CommentListResponse(
    val items: List<QuestionCommentResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

data class CreateCommentRequest(
    @field:NotBlank(message = "Comment content is required.")
    @field:Size(max = 1000, message = "Comment is too long.")
    val content: String,
    val parentCommentId: Long? = null,
)

data class UpdateSocialProfileRequest(
    @field:Size(max = 30, message = "Username must be 30 characters or less.")
    val username: String? = null,
    @field:Size(max = 50, message = "Display name must be 50 characters or less.")
    val displayName: String? = null,
    @field:Size(max = 300, message = "Bio must be 300 characters or less.")
    val bio: String? = null,
    @field:Size(max = 500, message = "Avatar URL is too long.")
    val avatarUrl: String? = null,
)
