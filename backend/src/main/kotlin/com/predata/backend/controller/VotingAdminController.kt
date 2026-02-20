package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.PauseService
import com.predata.backend.service.VotingCircuitBreaker
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 투표 관리자 컨트롤러
 * - 투표 일시 중지/재개
 * - 서킷브레이커 상태 조회
 */
@RestController
@RequestMapping("/api/admin/voting")
class VotingAdminController(
    private val pauseService: PauseService,
    private val circuitBreaker: VotingCircuitBreaker
) {

    /**
     * 특정 질문 투표 일시 중지
     * POST /api/admin/voting/pause/{questionId}
     */
    @PostMapping("/pause/{questionId}")
    fun pauseVoting(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<VotingPauseResponse>> {
        pauseService.pauseVoting(questionId)
        return ResponseEntity.ok(
            ApiEnvelope.ok(VotingPauseResponse(message = "Voting has been paused.", questionId = questionId))
        )
    }

    /**
     * 특정 질문 투표 재개
     * POST /api/admin/voting/resume/{questionId}
     */
    @PostMapping("/resume/{questionId}")
    fun resumeVoting(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<VotingPauseResponse>> {
        pauseService.resumeVoting(questionId)
        return ResponseEntity.ok(
            ApiEnvelope.ok(VotingPauseResponse(message = "Voting has been resumed.", questionId = questionId))
        )
    }

    /**
     * 전체 투표 일시 중지
     * POST /api/admin/voting/pause-all
     */
    @PostMapping("/pause-all")
    fun pauseAll(): ResponseEntity<ApiEnvelope<VotingPauseResponse>> {
        pauseService.pauseAll()
        return ResponseEntity.ok(
            ApiEnvelope.ok(VotingPauseResponse(message = "All voting has been paused."))
        )
    }

    /**
     * 전체 투표 재개
     * POST /api/admin/voting/resume-all
     */
    @PostMapping("/resume-all")
    fun resumeAll(): ResponseEntity<ApiEnvelope<VotingPauseResponse>> {
        pauseService.resumeAll()
        return ResponseEntity.ok(
            ApiEnvelope.ok(VotingPauseResponse(message = "All voting has been resumed."))
        )
    }

    /**
     * 투표 시스템 상태 조회
     * GET /api/admin/voting/status
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<ApiEnvelope<VotingSystemStatusResponse>> {
        val pauseStatus = pauseService.getAllPauseStatus()
        val circuitBreakerStats = circuitBreaker.getStats()

        return ResponseEntity.ok(
            ApiEnvelope.ok(VotingSystemStatusResponse(pauseStatus = pauseStatus, circuitBreaker = circuitBreakerStats))
        )
    }

    /**
     * 서킷브레이커 수동 리셋
     * POST /api/admin/voting/circuit-breaker/reset
     */
    @PostMapping("/circuit-breaker/reset")
    fun resetCircuitBreaker(): ResponseEntity<ApiEnvelope<VotingPauseResponse>> {
        circuitBreaker.reset()
        return ResponseEntity.ok(
            ApiEnvelope.ok(VotingPauseResponse(message = "Circuit breaker has been reset."))
        )
    }
}

data class VotingPauseResponse(
    val message: String,
    val questionId: Long? = null
)

data class VotingSystemStatusResponse(
    val pauseStatus: Map<String, Any>,
    val circuitBreaker: Map<String, Any>
)
