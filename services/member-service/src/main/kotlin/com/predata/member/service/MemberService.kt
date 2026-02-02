package com.predata.member.service

import com.predata.common.domain.Tier
import com.predata.member.domain.Member
import com.predata.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository
) {

    @Transactional
    fun createMember(request: CreateMemberRequest): Member {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다.")
        }

        val member = Member(
            email = request.email,
            countryCode = request.countryCode,
            jobCategory = request.jobCategory,
            ageGroup = request.ageGroup,
            tier = Tier.BRONZE
        )

        return memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun getMember(memberId: Long): Member {
        return memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }
    }

    @Transactional(readOnly = true)
    fun getMemberByEmail(email: String): Member? {
        return memberRepository.findByEmail(email)
    }

    @Transactional(readOnly = true)
    fun getMemberByWallet(walletAddress: String): Member? {
        return memberRepository.findByWalletAddress(walletAddress)
    }

    @Transactional
    fun connectWallet(memberId: Long, walletAddress: String): Member {
        val member = getMember(memberId)
        
        if (memberRepository.existsByWalletAddress(walletAddress)) {
            throw IllegalArgumentException("이미 연결된 지갑 주소입니다.")
        }

        member.walletAddress = walletAddress
        return memberRepository.save(member)
    }

    @Transactional
    fun deductPoints(memberId: Long, amount: Long): Member {
        val member = getMember(memberId)
        
        if (member.pointBalance < amount) {
            throw IllegalArgumentException("포인트가 부족합니다.")
        }

        member.pointBalance -= amount
        return memberRepository.save(member)
    }

    @Transactional
    fun addPoints(memberId: Long, amount: Long): Member {
        val member = getMember(memberId)
        member.pointBalance += amount
        return memberRepository.save(member)
    }
}

data class CreateMemberRequest(
    val email: String,
    val countryCode: String,
    val jobCategory: String? = null,
    val ageGroup: Int? = null
)
