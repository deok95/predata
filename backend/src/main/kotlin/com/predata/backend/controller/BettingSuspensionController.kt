package com.predata.backend.controller

import com.predata.backend.service.BettingSuspensionService
import com.predata.backend.service.BettingSuspensionStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/betting")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class BettingSuspensionController(
    private val bettingSuspensionService: BettingSuspensionService
) {

    /**
     * 질문 ID로 베팅 중지 상태 확인
     * GET /api/betting/suspension/question/{questionId}
     */
    @GetMapping("/suspension/question/{questionId}")
    fun checkSuspensionByQuestion(@PathVariable questionId: Long): ResponseEntity<BettingSuspensionStatus> {
        val status = bettingSuspensionService.isBettingSuspendedByQuestionId(questionId)
        return ResponseEntity.ok(status)
    }

    /**
     * 경기 ID로 베팅 중지 상태 확인
     * GET /api/betting/suspension/match/{matchId}
     */
    @GetMapping("/suspension/match/{matchId}")
    fun checkSuspensionByMatch(@PathVariable matchId: Long): ResponseEntity<Map<String, Any>> {
        val isSuspended = bettingSuspensionService.isBettingSuspended(matchId)
        val remainingTime = if (isSuspended) {
            bettingSuspensionService.getRemainingCooldownTime(matchId)
        } else {
            0
        }
        
        return ResponseEntity.ok(mapOf(
            "suspended" to isSuspended,
            "remainingSeconds" to remainingTime
        ))
    }
}
