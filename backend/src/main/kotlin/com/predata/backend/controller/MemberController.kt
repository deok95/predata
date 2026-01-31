package com.predata.backend.controller

import com.predata.backend.domain.Member
import com.predata.backend.repository.MemberRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = ["http://localhost:3000"])
class MemberController(
    private val memberRepository: MemberRepository
) {

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
     * 회원 생성 (회원가입)
     * POST /api/members
     */
    @PostMapping
    fun createMember(@RequestBody request: CreateMemberRequest): ResponseEntity<MemberResponse> {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        // 새 회원 생성
        val member = Member(
            email = request.email,
            countryCode = request.countryCode,
            jobCategory = request.jobCategory,
            ageGroup = request.ageGroup,
            tierWeight = BigDecimal("1.00"),
            pointBalance = 10000 // 초기 포인트
        )

        val savedMember = memberRepository.save(member)
        return ResponseEntity.ok(MemberResponse.from(savedMember))
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
    val email: String,
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
