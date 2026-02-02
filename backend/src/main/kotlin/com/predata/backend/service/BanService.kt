package com.predata.backend.service

import com.predata.backend.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BanService(
    private val memberRepository: MemberRepository
) {
    private val logger = LoggerFactory.getLogger(BanService::class.java)

    /**
     * 유저 밴 처리
     */
    @Transactional
    fun banMember(memberId: Long, reason: String): BanResult {
        val member = memberRepository.findById(memberId).orElse(null)
            ?: return BanResult(false, "회원을 찾을 수 없습니다.")

        if (member.isBanned) {
            return BanResult(false, "이미 정지된 계정입니다.")
        }

        member.isBanned = true
        member.banReason = reason
        member.bannedAt = LocalDateTime.now()
        memberRepository.save(member)

        logger.info("Member banned: id=$memberId, reason=$reason")
        return BanResult(true, "계정이 정지되었습니다.", memberId)
    }

    /**
     * 유저 밴 해제
     */
    @Transactional
    fun unbanMember(memberId: Long): BanResult {
        val member = memberRepository.findById(memberId).orElse(null)
            ?: return BanResult(false, "회원을 찾을 수 없습니다.")

        if (!member.isBanned) {
            return BanResult(false, "정지되지 않은 계정입니다.")
        }

        member.isBanned = false
        member.banReason = null
        member.bannedAt = null
        memberRepository.save(member)

        logger.info("Member unbanned: id=$memberId")
        return BanResult(true, "정지가 해제되었습니다.", memberId)
    }

    /**
     * 밴 여부 확인
     */
    fun isBanned(memberId: Long): Boolean {
        val member = memberRepository.findById(memberId).orElse(null) ?: return false
        return member.isBanned
    }

    /**
     * 정지된 유저 목록
     */
    fun getBannedMembers(): List<BannedMemberInfo> {
        return memberRepository.findByIsBannedTrue().map { member ->
            BannedMemberInfo(
                memberId = member.id ?: 0,
                email = member.email,
                banReason = member.banReason,
                bannedAt = member.bannedAt?.toString(),
                signupIp = member.signupIp,
                lastIp = member.lastIp
            )
        }
    }
}

data class BanResult(
    val success: Boolean,
    val message: String,
    val memberId: Long? = null
)

data class BannedMemberInfo(
    val memberId: Long,
    val email: String,
    val banReason: String?,
    val bannedAt: String?,
    val signupIp: String?,
    val lastIp: String?
)
