package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.service.VotingPassPurchaseResponse
import com.predata.backend.service.VotingPassService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
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
    fun purchaseVotingPass(
        httpRequest: HttpServletRequest,
        @RequestBody(required = false) request: VotingPassPurchaseRequest? = null // backward compatible; ignored
    ): ResponseEntity<VotingPassPurchaseResponse> {
        val authenticatedMemberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                VotingPassPurchaseResponse(
                    success = false,
                    hasVotingPass = false,
                    remainingBalance = 0.0,
                    message = "인증이 필요합니다."
                )
            )

        return try {
            // Never trust memberId in body; enforce JWT subject.
            val response = votingPassService.purchaseVotingPass(authenticatedMemberId)
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
