package com.predata.backend.service

import com.predata.backend.repository.MemberRepository
import com.predata.backend.util.IpUtil
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service

@Service
class ClientIpService(
    private val memberRepository: MemberRepository
) {
    fun extractClientIp(request: HttpServletRequest): String {
        return IpUtil.extractClientIp(request)
    }

    fun updateMemberLastIp(memberId: Long, ip: String) {
        try {
            memberRepository.findById(memberId).ifPresent { member ->
                member.lastIp = ip
                memberRepository.save(member)
            }
        } catch (_: Exception) {
            // IP 업데이트 실패는 핵심 비즈니스를 막지 않음
        }
    }
}
