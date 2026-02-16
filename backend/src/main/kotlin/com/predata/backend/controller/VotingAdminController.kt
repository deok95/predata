package com.predata.backend.controller

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
    fun pauseVoting(@PathVariable questionId: Long): ResponseEntity<Map<String, Any>> {
        pauseService.pauseVoting(questionId)
        return ResponseEntity.ok(
            mapOf(
                "message" to "투표가 일시 중지되었습니다.",
                "questionId" to questionId
            )
        )
    }

    /**
     * 특정 질문 투표 재개
     * POST /api/admin/voting/resume/{questionId}
     */
    @PostMapping("/resume/{questionId}")
    fun resumeVoting(@PathVariable questionId: Long): ResponseEntity<Map<String, Any>> {
        pauseService.resumeVoting(questionId)
        return ResponseEntity.ok(
            mapOf(
                "message" to "투표가 재개되었습니다.",
                "questionId" to questionId
            )
        )
    }

    /**
     * 전체 투표 일시 중지
     * POST /api/admin/voting/pause-all
     */
    @PostMapping("/pause-all")
    fun pauseAll(): ResponseEntity<Map<String, Any>> {
        pauseService.pauseAll()
        return ResponseEntity.ok(
            mapOf(
                "message" to "모든 투표가 일시 중지되었습니다."
            )
        )
    }

    /**
     * 전체 투표 재개
     * POST /api/admin/voting/resume-all
     */
    @PostMapping("/resume-all")
    fun resumeAll(): ResponseEntity<Map<String, Any>> {
        pauseService.resumeAll()
        return ResponseEntity.ok(
            mapOf(
                "message" to "모든 투표가 재개되었습니다."
            )
        )
    }

    /**
     * 투표 시스템 상태 조회
     * GET /api/admin/voting/status
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        val pauseStatus = pauseService.getAllPauseStatus()
        val circuitBreakerStats = circuitBreaker.getStats()

        return ResponseEntity.ok(
            mapOf(
                "pauseStatus" to pauseStatus,
                "circuitBreaker" to circuitBreakerStats
            )
        )
    }

    /**
     * 서킷브레이커 수동 리셋
     * POST /api/admin/voting/circuit-breaker/reset
     */
    @PostMapping("/circuit-breaker/reset")
    fun resetCircuitBreaker(): ResponseEntity<Map<String, Any>> {
        circuitBreaker.reset()
        return ResponseEntity.ok(
            mapOf(
                "message" to "서킷브레이커가 리셋되었습니다."
            )
        )
    }
}
