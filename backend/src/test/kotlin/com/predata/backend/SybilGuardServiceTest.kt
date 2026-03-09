package com.predata.backend

import com.predata.backend.domain.Member
import com.predata.backend.domain.VoteCommit
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.VoteCommitRepository
import com.predata.backend.service.SybilGuardService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SybilGuardServiceTest {

    @Autowired
    private lateinit var sybilGuardService: SybilGuardService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var voteCommitRepository: VoteCommitRepository

    @Test
    fun `eligible member passes account age and vote history checks`() {
        val member = memberRepository.save(
            Member(
                email = "sybil-eligible-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                createdAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(10)
            )
        )

        val memberId = member.id ?: error("member id required")
        listOf(101L, 102L, 103L).forEachIndexed { idx, questionId ->
            voteCommitRepository.save(
                VoteCommit(
                    memberId = memberId,
                    questionId = questionId,
                    commitHash = "a".repeat(63) + idx.toString()
                )
            )
        }

        assertTrue(sybilGuardService.isEligibleForReward(memberId))
    }

    @Test
    fun `ineligible member fails when age or vote count is insufficient`() {
        val youngMember = memberRepository.save(
            Member(
                email = "sybil-young-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                createdAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(1)
            )
        )

        val noHistoryMember = memberRepository.save(
            Member(
                email = "sybil-no-history-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                createdAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(10)
            )
        )

        assertFalse(sybilGuardService.isEligibleForReward(youngMember.id!!))
        assertFalse(sybilGuardService.isEligibleForReward(noHistoryMember.id!!))
    }
}
