package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteCommitResponse
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.VoteRevealResponse
import com.predata.backend.service.VoteCommitService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Commit-Reveal 투표 컨트롤러
 * - POST /api/votes/commit: 투표 커밋 (해시 저장)
 * - POST /api/votes/reveal: 투표 공개 (선택 검증 및 저장)
 */
@RestController
@RequestMapping("/api/votes")
class VoteController(
    private val voteCommitService: VoteCommitService
) {

    /**
     * 1단계: Commit (투표 해시 저장)
     * - JWT 인증 필수
     * - commitHash = SHA-256(choice + salt)
     */
    @PostMapping("/commit")
    fun commit(
        @Valid @RequestBody request: VoteCommitRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<VoteCommitResponse> {
        val authenticatedMemberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                VoteCommitResponse(
                    success = false,
                    message = "인증이 필요합니다."
                )
            )

        val response = voteCommitService.commit(authenticatedMemberId, request)
        val status = if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(response)
    }

    /**
     * 2단계: Reveal (투표 공개)
     * - JWT 인증 필수
     * - SHA-256(choice + salt) == commitHash 검증
     */
    @PostMapping("/reveal")
    fun reveal(
        @Valid @RequestBody request: VoteRevealRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<VoteRevealResponse> {
        val authenticatedMemberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                VoteRevealResponse(
                    success = false,
                    message = "인증이 필요합니다."
                )
            )

        val response = voteCommitService.reveal(authenticatedMemberId, request)
        val status = if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(response)
    }
}
