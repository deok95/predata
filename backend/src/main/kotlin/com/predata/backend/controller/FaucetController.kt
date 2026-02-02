package com.predata.backend.controller

import com.predata.backend.service.FaucetClaimResponse
import com.predata.backend.service.FaucetService
import com.predata.backend.service.FaucetStatusResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/faucet")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class FaucetController(
    private val faucetService: FaucetService
) {

    /**
     * 일일 포인트 수령
     * POST /api/faucet/claim/{memberId}
     */
    @PostMapping("/claim/{memberId}")
    fun claimDailyPoints(@PathVariable memberId: Long): ResponseEntity<FaucetClaimResponse> {
        val result = faucetService.claimDailyPoints(memberId)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result)
        }
    }

    /**
     * 수령 상태 조회
     * GET /api/faucet/status/{memberId}
     */
    @GetMapping("/status/{memberId}")
    fun getFaucetStatus(@PathVariable memberId: Long): ResponseEntity<FaucetStatusResponse> {
        val status = faucetService.getFaucetStatus(memberId)
        return ResponseEntity.ok(status)
    }
}
