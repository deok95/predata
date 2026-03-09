package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.*
import com.predata.backend.service.SocialService
import com.predata.backend.util.authenticatedMemberId
import com.predata.backend.util.authenticatedMemberIdOrNull
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "member-social", description = "Social APIs")
@RequestMapping("/api")
class SocialController(
    private val socialService: SocialService,
) {
    @GetMapping("/users/{id}")
    fun getProfile(
        @PathVariable id: Long,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<SocialProfileResponse>> {
        val requesterId = request.authenticatedMemberIdOrNull()
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.getProfile(id, requesterId)))
    }

    @PutMapping("/users/me/profile")
    fun updateMyProfile(
        @Valid @RequestBody body: UpdateSocialProfileRequest,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<SocialProfileResponse>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.updateMyProfile(memberId, body)))
    }

    @PostMapping("/users/{id}/follow")
    fun follow(
        @PathVariable id: Long,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<FollowActionResponse>> {
        val followerId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.follow(followerId, id)))
    }

    @DeleteMapping("/users/{id}/follow")
    fun unfollow(
        @PathVariable id: Long,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<FollowActionResponse>> {
        val followerId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.unfollow(followerId, id)))
    }

    @GetMapping("/users/{id}/followers")
    fun getFollowers(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int,
    ): ResponseEntity<ApiEnvelope<FollowListResponse>> {
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.getFollowers(id, page, size)))
    }

    @GetMapping("/users/{id}/following")
    fun getFollowing(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int,
    ): ResponseEntity<ApiEnvelope<FollowListResponse>> {
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.getFollowing(id, page, size)))
    }

    @GetMapping("/questions/{id}/comments")
    fun getComments(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<CommentListResponse>> {
        val requesterId = request.authenticatedMemberIdOrNull()
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.getQuestionComments(id, requesterId, page, size)))
    }

    @PostMapping("/questions/{id}/comments")
    fun createComment(
        @PathVariable id: Long,
        @Valid @RequestBody body: CreateCommentRequest,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<QuestionCommentResponse>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(socialService.createQuestionComment(id, memberId, body)))
    }

    @DeleteMapping("/questions/{questionId}/comments/{commentId}")
    fun deleteComment(
        @PathVariable questionId: Long,
        @PathVariable commentId: Long,
        request: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<Unit>> {
        val memberId = request.authenticatedMemberId()
        socialService.deleteComment(questionId, commentId, memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(Unit))
    }
}
