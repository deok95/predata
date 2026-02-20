package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.ReferralResult
import com.predata.backend.service.ReferralService
import com.predata.backend.service.ReferralStatsResponse
import com.predata.backend.util.authenticatedMemberId
import com.predata.backend.util.IpUtil
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/referrals")
class ReferralController(
    private val referralService: ReferralService
) {

    @GetMapping("/stats")
    fun getStats(request: HttpServletRequest): ResponseEntity<ApiEnvelope<ReferralStatsResponse>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(referralService.getReferralStats(memberId)))
    }

    @GetMapping("/code")
    fun getMyCode(request: HttpServletRequest): ResponseEntity<ApiEnvelope<ReferralCodeResponse>> {
        val memberId = request.authenticatedMemberId()
        val code = referralService.getReferralCode(memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(ReferralCodeResponse(code = code)))
    }

    @PostMapping("/apply")
    fun applyReferral(
        @RequestBody req: ApplyReferralRequest,
        request: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<ReferralResult>> {
        val memberId = request.authenticatedMemberId()
        val clientIp = IpUtil.extractClientIp(request)
        val result = referralService.applyReferral(memberId, req.referralCode, clientIp)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }
}

data class ApplyReferralRequest(val referralCode: String)
data class ReferralCodeResponse(val code: String)
