package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.VotingPassPurchaseResponse
import com.predata.backend.service.VotingPassService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "voting", description = "Voting pass purchase APIs")
@RequestMapping("/api/voting-pass")
class VotingPassController(
    private val votingPassService: VotingPassService,
) {
    @PostMapping("/purchase")
    fun purchaseVotingPass(
        httpRequest: HttpServletRequest,
    ): ApiEnvelope<VotingPassPurchaseResponse> {
        val memberId = httpRequest.authenticatedMemberId()
        val result = votingPassService.purchaseVotingPass(memberId)
        return ApiEnvelope.ok(result)
    }
}
