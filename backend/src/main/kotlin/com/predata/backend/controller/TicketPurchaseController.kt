package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.VotingPassPurchaseResponse
import com.predata.backend.service.VotingPassService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/voting-pass")
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
    ): ResponseEntity<ApiEnvelope<VotingPassPurchaseResponse>> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        // Never trust memberId in body; enforce JWT subject.
        val response = votingPassService.purchaseVotingPass(authenticatedMemberId)
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }
}

data class VotingPassPurchaseRequest(
    val memberId: Long
)
