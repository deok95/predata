package com.predata.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteCommitResponse
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.VoteRevealResponse
import com.predata.backend.exception.ServiceUnavailableException
import com.predata.backend.service.IdempotencyService
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
 * - GET /api/votes/results/{questionId}: 투표 결과 조회
 * - GET /api/votes/status/{questionId}: 투표 상태 조회
 */
@RestController
@RequestMapping("/api/votes")
class VoteController(
    private val voteCommitService: VoteCommitService,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper
) {

    /**
     * 1단계: Commit (투표 해시 저장)
     * - JWT 인증 필수
     * - commitHash = SHA-256(choice + salt)
     * - X-Idempotency-Key 지원 (중복 요청 방지)
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
                    message = "Authentication required."
                )
            )

        // Idempotency 체크
        val idempotencyKey = httpRequest.getHeader("X-Idempotency-Key")
        if (idempotencyKey != null) {
            val requestBody = objectMapper.writeValueAsString(request)
            val existing = idempotencyService.checkAndStore(
                idempotencyKey,
                authenticatedMemberId,
                "/api/votes/commit",
                requestBody
            )

            // 중복 요청 → 캐시된 응답 반환
            if (existing != null) {
                val cachedResponse = objectMapper.readValue(
                    existing.responseBody,
                    VoteCommitResponse::class.java
                )
                return ResponseEntity.status(existing.responseStatus).body(cachedResponse)
            }
        }

        // 정상 처리
        return try {
            val response = voteCommitService.commit(authenticatedMemberId, request)
            val status = if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST

            // Idempotency 저장
            if (idempotencyKey != null) {
                val requestBody = objectMapper.writeValueAsString(request)
                val responseBody = objectMapper.writeValueAsString(response)
                idempotencyService.store(
                    idempotencyKey,
                    authenticatedMemberId,
                    "/api/votes/commit",
                    requestBody,
                    responseBody,
                    status.value()
                )
            }

            ResponseEntity.status(status).body(response)
        } catch (e: ServiceUnavailableException) {
            // 시스템 점검 중 (pause/circuit breaker) → 503 Service Unavailable
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                VoteCommitResponse(
                    success = false,
                    message = e.message ?: "System is under maintenance."
                )
            )
        } catch (e: IllegalStateException) {
            // Phase 검증 실패 → 409 Conflict
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                VoteCommitResponse(
                    success = false,
                    message = e.message ?: "This action cannot be performed in the current state."
                )
            )
        }
    }

    /**
     * 2단계: Reveal (투표 공개)
     * - JWT 인증 필수
     * - SHA-256(choice + salt) == commitHash 검증
     * - X-Idempotency-Key 지원 (중복 요청 방지)
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
                    message = "Authentication required."
                )
            )

        // Idempotency 체크
        val idempotencyKey = httpRequest.getHeader("X-Idempotency-Key")
        if (idempotencyKey != null) {
            val requestBody = objectMapper.writeValueAsString(request)
            val existing = idempotencyService.checkAndStore(
                idempotencyKey,
                authenticatedMemberId,
                "/api/votes/reveal",
                requestBody
            )

            // 중복 요청 → 캐시된 응답 반환
            if (existing != null) {
                val cachedResponse = objectMapper.readValue(
                    existing.responseBody,
                    VoteRevealResponse::class.java
                )
                return ResponseEntity.status(existing.responseStatus).body(cachedResponse)
            }
        }

        // 정상 처리
        return try {
            val response = voteCommitService.reveal(authenticatedMemberId, request)
            val status = if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST

            // Idempotency 저장
            if (idempotencyKey != null) {
                val requestBody = objectMapper.writeValueAsString(request)
                val responseBody = objectMapper.writeValueAsString(response)
                idempotencyService.store(
                    idempotencyKey,
                    authenticatedMemberId,
                    "/api/votes/reveal",
                    requestBody,
                    responseBody,
                    status.value()
                )
            }

            ResponseEntity.status(status).body(response)
        } catch (e: ServiceUnavailableException) {
            // 시스템 점검 중 (pause/circuit breaker) → 503 Service Unavailable
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                VoteRevealResponse(
                    success = false,
                    message = e.message ?: "System is under maintenance."
                )
            )
        } catch (e: IllegalStateException) {
            // Phase 검증 실패 → 409 Conflict
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                VoteRevealResponse(
                    success = false,
                    message = e.message ?: "This action cannot be performed in the current state."
                )
            )
        }
    }

    /**
     * 투표 결과 조회 (Reveal 종료 후에만 공개)
     */
    @GetMapping("/results/{questionId}")
    fun getResults(@PathVariable questionId: Long): ResponseEntity<Map<String, Any>> {
        return try {
            val result = voteCommitService.getResults(questionId)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed."))
            )
        }
    }

    /**
     * 투표 상태 조회 (phase, 참여자 수만 공개)
     */
    @GetMapping("/status/{questionId}")
    fun getStatus(@PathVariable questionId: Long): ResponseEntity<Map<String, Any>> {
        val status = voteCommitService.getStatus(questionId)
        return ResponseEntity.ok(status)
    }
}
