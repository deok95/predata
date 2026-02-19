package com.predata.backend.controller

import com.predata.backend.service.BanService
import com.predata.backend.service.IpTrackingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 어뷰징 관리 API (관리자용)
 */
@RestController
@RequestMapping("/api/admin/abuse")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class BanController(
    private val banService: BanService,
    private val ipTrackingService: IpTrackingService
) {

    /**
     * 유저 밴
     * POST /api/admin/abuse/ban
     */
    @PostMapping("/ban")
    fun banMember(@RequestBody request: BanRequest): ResponseEntity<Any> {
        val result = banService.banMember(request.memberId, request.reason)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * 유저 밴 해제
     * POST /api/admin/abuse/unban
     */
    @PostMapping("/unban")
    fun unbanMember(@RequestBody request: UnbanRequest): ResponseEntity<Any> {
        val result = banService.unbanMember(request.memberId)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * 정지된 유저 목록
     * GET /api/admin/abuse/banned
     */
    @GetMapping("/banned")
    fun getBannedMembers(): ResponseEntity<Any> {
        return ResponseEntity.ok(banService.getBannedMembers())
    }

    /**
     * IP 기반 다중계정 의심 리포트
     * GET /api/admin/abuse/multi-account-report
     */
    @GetMapping("/multi-account-report")
    fun getMultiAccountReport(): ResponseEntity<Any> {
        return ResponseEntity.ok(ipTrackingService.detectMultiAccounts())
    }

    /**
     * 특정 IP의 연결 계정 조회
     * GET /api/admin/abuse/ip-lookup?ip=192.168.1.1
     */
    @GetMapping("/ip-lookup")
    fun lookupIp(@RequestParam ip: String): ResponseEntity<Any> {
        return ResponseEntity.ok(ipTrackingService.getAccountsByIp(ip))
    }

    /**
     * 특정 유저의 IP 이력 조회
     * GET /api/admin/abuse/member-ips/{memberId}
     */
    @GetMapping("/member-ips/{memberId}")
    fun getMemberIps(@PathVariable memberId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok(ipTrackingService.getIpHistoryForMember(memberId))
    }
}

data class BanRequest(
    val memberId: Long,
    val reason: String
)

data class UnbanRequest(
    val memberId: Long
)
