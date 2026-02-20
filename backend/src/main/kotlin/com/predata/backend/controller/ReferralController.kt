package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
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
    fun getStats(request: HttpServletRequest): ResponseEntity<ReferralStatsResponse> {
        return try {
            val memberId = request.authenticatedMemberId()
            ResponseEntity.ok(referralService.getReferralStats(memberId))
        } catch (e: Exception) {
            // Return default referral stats on any error
            ResponseEntity.ok(ReferralStatsResponse(
                referralCode = "N/A",
                totalReferrals = 0L,
                totalPointsEarned = 0L,
                referees = emptyList()
            ))
        }
    }

    @GetMapping("/code")
    fun getMyCode(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val memberId = request.authenticatedMemberId()
        val code = referralService.getReferralCode(memberId)
        return ResponseEntity.ok(mapOf("code" to code))
    }

    @PostMapping("/apply")
    fun applyReferral(
        @RequestBody req: ApplyReferralRequest,
        request: HttpServletRequest
    ): ResponseEntity<ReferralResult> {
        val memberId = request.authenticatedMemberId()
        val clientIp = IpUtil.extractClientIp(request)
        val result = referralService.applyReferral(memberId, req.referralCode, clientIp)
        return ResponseEntity.ok(result)
    }
}

data class ApplyReferralRequest(
    val referralCode: String
)
