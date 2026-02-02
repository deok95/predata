package com.predata.backend.controller

import com.predata.backend.domain.Member
import com.predata.backend.repository.MemberRepository
import com.predata.backend.service.IpTrackingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
class MemberController(
    private val memberRepository: MemberRepository,
    private val ipTrackingService: IpTrackingService
) {
    private val logger = LoggerFactory.getLogger(MemberController::class.java)

    /**
     * 이메일로 회원 조회
     * GET /api/members/by-email?email=test@example.com
     */
    @GetMapping("/by-email")
    fun getMemberByEmail(@RequestParam email: String): ResponseEntity<MemberResponse> {
        val member = memberRepository.findByEmail(email)
        
        return if (member.isPresent) {
            ResponseEntity.ok(MemberResponse.from(member.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 지갑 주소로 회원 조회
     * GET /api/members/by-wallet?address=0x1234...
     */
    @GetMapping("/by-wallet")
    fun getMemberByWallet(@RequestParam address: String): ResponseEntity<MemberResponse> {
        val member = memberRepository.findByWalletAddress(address)
        
        return if (member.isPresent) {
            ResponseEntity.ok(MemberResponse.from(member.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 회원 생성 (회원가입)
     * POST /api/members
     */
    @PostMapping
    fun createMember(
        @Valid @RequestBody request: CreateMemberRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MemberResponse> {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        // IP 추출
        val clientIp = extractClientIp(httpRequest)

        // 동일 IP 다중가입 경고 (로그)
        val existingFromIp = ipTrackingService.checkSignupIp(clientIp)
        if (existingFromIp >= 3) {
            logger.warn("Suspicious signup: IP=$clientIp already has $existingFromIp accounts")
        }

        // 새 회원 생성 (IP 기록 포함)
        val member = Member(
            email = request.email,
            walletAddress = request.walletAddress,
            countryCode = request.countryCode,
            jobCategory = request.jobCategory,
            ageGroup = request.ageGroup,
            tierWeight = BigDecimal("1.00"),
            pointBalance = 10000, // 초기 포인트
            signupIp = clientIp,
            lastIp = clientIp
        )

        val savedMember = memberRepository.save(member)
        return ResponseEntity.ok(MemberResponse.from(savedMember))
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) return forwarded.split(",").first().trim()
        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) return realIp.trim()
        return request.remoteAddr ?: "unknown"
    }

    /**
     * 회원 정보 조회 (ID로)
     * GET /api/members/{id}
     */
    @GetMapping("/{id}")
    fun getMember(@PathVariable id: Long): ResponseEntity<MemberResponse> {
        val member = memberRepository.findById(id)
        
        return if (member.isPresent) {
            ResponseEntity.ok(MemberResponse.from(member.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

/**
 * DTO: 회원 생성 요청
 */
data class CreateMemberRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이 아닙니다.")
    val email: String,

    @field:Size(min = 42, max = 42, message = "지갑 주소는 42자여야 합니다.")
    val walletAddress: String? = null,

    @field:NotBlank(message = "국가 코드는 필수입니다.")
    @field:Size(min = 2, max = 2, message = "국가 코드는 2자리여야 합니다.")
    val countryCode: String,

    val jobCategory: String,
    val ageGroup: Int
)

/**
 * DTO: 회원 응답
 */
data class MemberResponse(
    val memberId: Long,
    val email: String,
    val walletAddress: String?,
    val countryCode: String,
    val jobCategory: String,
    val ageGroup: Int,
    val tier: String,
    val tierWeight: Double,
    val pointBalance: Long
) {
    companion object {
        fun from(member: Member): MemberResponse {
            return MemberResponse(
                memberId = member.id ?: 0,
                email = member.email,
                walletAddress = member.walletAddress,
                countryCode = member.countryCode,
                jobCategory = member.jobCategory ?: "",
                ageGroup = member.ageGroup ?: 0,
                tier = member.tier,
                tierWeight = member.tierWeight.toDouble(),
                pointBalance = member.pointBalance
            )
        }
    }
}
