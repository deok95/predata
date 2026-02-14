package com.predata.backend.controller

import com.predata.backend.service.VotingPassPurchaseResponse
import com.predata.backend.service.VotingPassService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/voting-pass")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class VotingPassController(
    private val votingPassService: VotingPassService
) {

    /**
     * 투표 패스 구매
     * POST /api/voting-pass/purchase
     */
    @PostMapping("/purchase")
    fun purchaseVotingPass(@RequestBody request: VotingPassPurchaseRequest): ResponseEntity<VotingPassPurchaseResponse> {
        return try {
            val response = votingPassService.purchaseVotingPass(request.memberId)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                VotingPassPurchaseResponse(
                    success = false,
                    hasVotingPass = false,
                    remainingBalance = 0.0,
                    message = e.message ?: "투표 패스 구매에 실패했습니다."
                )
            )
        }
    }
}

data class VotingPassPurchaseRequest(
    val memberId: Long
)
