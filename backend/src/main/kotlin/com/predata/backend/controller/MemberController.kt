package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.domain.Member
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.MemberRepository
import com.predata.backend.service.ClientIpService
import com.predata.backend.service.IpTrackingService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberRepository: MemberRepository,
    private val ipTrackingService: IpTrackingService,
    private val clientIpService: ClientIpService
) {
    private val logger = LoggerFactory.getLogger(MemberController::class.java)

    /**
     * ❌ REMOVED: /by-email 엔드포인트 제거 (보안상 위험)
     *
     * 이유:
     * 1. 인증 없이 이메일로 회원 정보 조회 가능 (프라이버시 침해)
     * 2. 사용자 열거(enumeration) 공격에 취약
     * 3. GDPR 위반 가능성
     * 4. 프론트엔드에서 미사용 (loginWithEmail 함수 정의되었으나 호출 안 됨)
     *
     * 대안:
     * - Admin이 필요한 경우: Admin 전용 컨트롤러로 이동 + AdminAuthInterceptor 적용
     * - 일반 사용자: /api/members/me 사용 (JWT 인증 필요)
     */

    /**
     * 지갑 주소로 회원 조회 (공개 정보만)
     * GET /api/members/by-wallet?address=0x1234...
     */
    @GetMapping("/by-wallet")
    fun getMemberByWallet(@RequestParam address: String): ResponseEntity<ApiEnvelope<PublicMemberResponse>> {
        val member = memberRepository.findByWalletAddress(address)

        return if (member.isPresent) {
            ResponseEntity.ok(ApiEnvelope.ok(PublicMemberResponse.from(member.get())))
        } else {
            throw NotFoundException("Member not found.")
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
    ): ResponseEntity<ApiEnvelope<MemberResponse>> {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already in use.")
        }

        // IP 추출
        val clientIp = clientIpService.extractClientIp(httpRequest)

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
            usdcBalance = BigDecimal.ZERO, // USDC 시스템: 초기 잔액 없음
            signupIp = clientIp,
            lastIp = clientIp
        )

        val savedMember = memberRepository.save(member)
        return ResponseEntity.ok(ApiEnvelope.ok(MemberResponse.from(savedMember)))
    }

    /**
     * 현재 로그인한 회원 정보 조회 (JWT 토큰 기반)
     * GET /api/members/me
     */
    @GetMapping("/me")
    fun getMyInfo(httpRequest: HttpServletRequest): ResponseEntity<ApiEnvelope<MemberResponse>> {
        val memberId = httpRequest.authenticatedMemberId()

        val member = memberRepository.findById(memberId)

        return if (member.isPresent) {
            ResponseEntity.ok(ApiEnvelope.ok(MemberResponse.from(member.get())))
        } else {
            throw NotFoundException("Member not found.")
        }
    }

    /**
     * 회원 정보 조회 (ID로) - 공개 정보만 반환
     * GET /api/members/{id}
     *
     * ⚠️ 보안: 이메일, 잔액, 역할 등 민감 정보는 제외됨
     */
    @GetMapping("/{id}")
    fun getMember(@PathVariable id: Long): ResponseEntity<ApiEnvelope<PublicMemberResponse>> {
        val member = memberRepository.findById(id)
            .orElseThrow { NotFoundException("Member not found.") }
        return ResponseEntity.ok(ApiEnvelope.ok(PublicMemberResponse.from(member)))
    }

    /**
     * 지갑 주소 연결/변경
     * PUT /api/members/wallet
     */
    @PutMapping("/wallet")
    fun updateWalletAddress(
        @Valid @RequestBody request: UpdateWalletRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<MemberResponse>> {
        val memberId = httpRequest.authenticatedMemberId()

        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        if (request.walletAddress != null && !request.walletAddress.matches(Regex("^0x[a-fA-F0-9]{40}$"))) {
            throw IllegalArgumentException("Invalid wallet address.")
        }

        if (request.walletAddress != null) {
            val existingMember = memberRepository.findByWalletAddress(request.walletAddress)
            if (existingMember.isPresent && existingMember.get().id != memberId) {
                throw ConflictException("Wallet already in use by another user.")
            }
        }

        member.walletAddress = request.walletAddress
        val updated = memberRepository.save(member)

        logger.info("지갑 주소 업데이트: memberId=$memberId, walletAddress=${request.walletAddress}")

        return ResponseEntity.ok(ApiEnvelope.ok(MemberResponse.from(updated)))
    }
}

/**
 * DTO: 회원 생성 요청
 */
data class CreateMemberRequest(
    @field:NotBlank(message = "Email is required.")
    @field:Email(message = "Invalid email format.")
    val email: String,

    @field:Size(min = 42, max = 42, message = "Wallet address must be 42 characters.")
    val walletAddress: String? = null,

    @field:NotBlank(message = "Country code is required.")
    @field:Size(min = 2, max = 2, message = "Country code must be 2 characters.")
    val countryCode: String,

    val jobCategory: String,
    val ageGroup: Int
)

/**
 * DTO: 지갑 주소 업데이트 요청
 */
data class UpdateWalletRequest(
    val walletAddress: String?
)

/**
 * DTO: 공개 회원 정보 (민감 정보 제외)
 * 용도: /members/{id}, /members/by-wallet 등 인증 없는 공개 API
 */
data class PublicMemberResponse(
    val memberId: Long,
    val walletAddress: String?,
    val countryCode: String,
    val tier: String,
    val tierWeight: Double,
    val hasVotingPass: Boolean
    // ❌ email 제외 (프라이버시)
    // ❌ usdcBalance 제외 (금융 정보)
    // ❌ role 제외 (보안)
    // ❌ jobCategory, ageGroup 제외 (개인정보)
) {
    companion object {
        fun from(member: Member): PublicMemberResponse {
            return PublicMemberResponse(
                memberId = member.id ?: 0,
                walletAddress = member.walletAddress,
                countryCode = member.countryCode,
                tier = member.tier,
                tierWeight = member.tierWeight.toDouble(),
                hasVotingPass = member.hasVotingPass
            )
        }
    }
}

/**
 * DTO: 인증된 회원 정보 (전체 정보)
 * 용도: /members/me (JWT 인증 필수)
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
    val usdcBalance: BigDecimal,
    val role: String,
    val hasVotingPass: Boolean
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
                usdcBalance = member.usdcBalance,
                role = member.role,
                hasVotingPass = member.hasVotingPass
            )
        }
    }
}
