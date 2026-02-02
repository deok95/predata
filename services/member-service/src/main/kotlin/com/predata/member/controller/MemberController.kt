package com.predata.member.controller

import com.predata.common.dto.ApiResponse
import com.predata.member.service.CreateMemberRequest
import com.predata.member.service.MemberService
import com.predata.member.dto.MemberResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = ["http://localhost:3000"])
class MemberController(
    private val memberService: MemberService
) {

    @PostMapping
    fun createMember(@RequestBody request: CreateMemberRequest): ApiResponse<MemberResponse> {
        val member = memberService.createMember(request)
        return ApiResponse(success = true, data = MemberResponse.from(member))
    }

    @GetMapping("/{id}")
    fun getMember(@PathVariable id: Long): ApiResponse<MemberResponse> {
        val member = memberService.getMember(id)
        return ApiResponse(success = true, data = MemberResponse.from(member))
    }

    @GetMapping("/by-email")
    fun getMemberByEmail(@RequestParam email: String): ApiResponse<MemberResponse> {
        val member = memberService.getMemberByEmail(email)
            ?: return ApiResponse(success = false, data = null, message = "회원을 찾을 수 없습니다.")
        return ApiResponse(success = true, data = MemberResponse.from(member))
    }

    @GetMapping("/by-wallet")
    fun getMemberByWallet(@RequestParam address: String): ApiResponse<MemberResponse> {
        val member = memberService.getMemberByWallet(address)
            ?: return ApiResponse(success = false, data = null, message = "회원을 찾을 수 없습니다.")
        return ApiResponse(success = true, data = MemberResponse.from(member))
    }

    @PostMapping("/{id}/wallet")
    fun connectWallet(
        @PathVariable id: Long,
        @RequestBody request: ConnectWalletRequest
    ): ApiResponse<MemberResponse> {
        val member = memberService.connectWallet(id, request.walletAddress)
        return ApiResponse(success = true, data = MemberResponse.from(member))
    }

    @PostMapping("/{id}/deduct-points")
    fun deductPoints(
        @PathVariable id: Long,
        @RequestBody request: com.predata.common.dto.PointsRequest
    ): ApiResponse<Unit> {
        memberService.deductPoints(id, request.amount)
        return ApiResponse(success = true)
    }

    @PostMapping("/{id}/add-points")
    fun addPoints(
        @PathVariable id: Long,
        @RequestBody request: com.predata.common.dto.PointsRequest
    ): ApiResponse<Unit> {
        memberService.addPoints(id, request.amount)
        return ApiResponse(success = true)
    }
}

data class ConnectWalletRequest(
    val walletAddress: String
)
