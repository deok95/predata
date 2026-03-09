package com.predata.backend.service.bot

import com.predata.backend.domain.Member
import com.predata.backend.domain.policy.BotMemberPolicy
import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BotMemberService(
    private val memberRepository: MemberRepository
) {
    @Transactional
    fun ensureBotMembers(): List<Member> {
        val existing = memberRepository.findAll().filter { BotMemberPolicy.isBotEmail(it.email) }
        val toCreate = BotMemberPolicy.missingBotCount(existing.size)
        if (toCreate == 0) return existing

        val newBots = (1..toCreate).map { i ->
            BotMemberPolicy.createBotMember(existing.size + i)
        }

        return existing + memberRepository.saveAll(newBots)
    }

    fun getBotMembers(): List<Member> {
        return memberRepository.findAll().filter { BotMemberPolicy.isBotEmail(it.email) }
    }

    fun isBot(memberId: Long): Boolean {
        return BotMemberPolicy.isBotRole(memberRepository.findById(memberId).orElse(null)?.role)
    }
}
