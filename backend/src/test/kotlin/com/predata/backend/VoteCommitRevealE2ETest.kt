package com.predata.backend

import com.predata.backend.domain.Choice
import com.predata.backend.domain.DailyTicket
import com.predata.backend.domain.Member
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.repository.DailyTicketRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.VoteCommitService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class VoteCommitRevealE2ETest {

    @Autowired
    private lateinit var voteCommitService: VoteCommitService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var dailyTicketRepository: DailyTicketRepository

    @Test
    fun `vote commit reveal e2e - success and error cases`() {
        val member = memberRepository.save(
            Member(
                email = "vote-commit-user-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                hasVotingPass = true
            )
        )
        val noPassMember = memberRepository.save(
            Member(
                email = "vote-commit-nopass-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                hasVotingPass = false
            )
        )
        val bannedMember = memberRepository.save(
            Member(
                email = "vote-commit-banned-${System.currentTimeMillis()}@example.com",
                countryCode = "KR",
                hasVotingPass = true,
                isBanned = true,
                banReason = "E2E test"
            )
        )
        val memberId = member.id!!
        val noPassMemberId = noPassMember.id!!
        val bannedMemberId = bannedMember.id!!

        dailyTicketRepository.save(
            DailyTicket(
                memberId = memberId,
                remainingCount = 5,
                resetDate = LocalDate.now()
            )
        )

        val questionA = createQuestion(VotingPhase.VOTING_COMMIT_OPEN)
        val questionB = createQuestion(VotingPhase.VOTING_COMMIT_OPEN)
        val questionC = createQuestion(VotingPhase.VOTING_COMMIT_OPEN)
        val questionAId = questionA.id!!
        val questionBId = questionB.id!!
        val questionCId = questionC.id!!

        // 1. commit 성공
        val saltA = "salt-a"
        val hashA = computeHash(questionAId, memberId, "YES", saltA)
        val commitSuccess = voteCommitService.commit(
            memberId,
            VoteCommitRequest(questionAId, hashA)
        )
        assertTrue(commitSuccess.success)
        assertNotNull(commitSuccess.voteCommitId)
        assertEquals(4, commitSuccess.remainingTickets)

        // 2. commit 중복
        val commitDuplicate = voteCommitService.commit(
            memberId,
            VoteCommitRequest(questionAId, hashA)
        )
        assertFalse(commitDuplicate.success)

        // 3. commit 투표패스 없는 유저
        val noPassHash = computeHash(questionAId, noPassMemberId, "YES", "salt-nopass")
        val commitNoPass = voteCommitService.commit(
            noPassMemberId,
            VoteCommitRequest(questionAId, noPassHash)
        )
        assertFalse(commitNoPass.success)

        // 4. commit 밴 유저
        val bannedHash = computeHash(questionAId, bannedMemberId, "YES", "salt-banned")
        val commitBanned = voteCommitService.commit(
            bannedMemberId,
            VoteCommitRequest(questionAId, bannedHash)
        )
        assertFalse(commitBanned.success)

        // 5. reveal 전에 question.votingPhase를 VOTING_REVEAL_OPEN으로 변경
        questionA.votingPhase = VotingPhase.VOTING_REVEAL_OPEN
        questionRepository.save(questionA)

        // 6. reveal 올바른 salt
        val revealSuccess = voteCommitService.reveal(
            memberId,
            VoteRevealRequest(questionAId, Choice.YES, saltA)
        )
        assertTrue(revealSuccess.success)

        // 7. reveal 잘못된 salt
        val saltB = "salt-b"
        val hashB = computeHash(questionBId, memberId, "NO", saltB)
        val commitForWrongSalt = voteCommitService.commit(
            memberId,
            VoteCommitRequest(questionBId, hashB)
        )
        assertTrue(commitForWrongSalt.success)
        questionB.votingPhase = VotingPhase.VOTING_REVEAL_OPEN
        questionRepository.save(questionB)

        val revealWrongSalt = voteCommitService.reveal(
            memberId,
            VoteRevealRequest(questionBId, Choice.NO, "wrong-salt")
        )
        assertFalse(revealWrongSalt.success)

        // 8. reveal 중복
        val saltC = "salt-c"
        val hashC = computeHash(questionCId, memberId, "YES", saltC)
        val commitForDuplicateReveal = voteCommitService.commit(
            memberId,
            VoteCommitRequest(questionCId, hashC)
        )
        assertTrue(commitForDuplicateReveal.success)
        questionC.votingPhase = VotingPhase.VOTING_REVEAL_OPEN
        questionRepository.save(questionC)

        val revealFirst = voteCommitService.reveal(
            memberId,
            VoteRevealRequest(questionCId, Choice.YES, saltC)
        )
        assertTrue(revealFirst.success)

        val revealDuplicate = voteCommitService.reveal(
            memberId,
            VoteRevealRequest(questionCId, Choice.YES, saltC)
        )
        assertFalse(revealDuplicate.success)
    }

    private fun createQuestion(votingPhase: VotingPhase): Question {
        return questionRepository.save(
            Question(
                title = "Vote commit reveal E2E question ${System.nanoTime()}",
                category = "TEST",
                status = QuestionStatus.VOTING,
                type = QuestionType.VERIFIABLE,
                votingPhase = votingPhase,
                votingEndAt = LocalDateTime.now().plusHours(2),
                bettingStartAt = LocalDateTime.now().plusHours(3),
                bettingEndAt = LocalDateTime.now().plusHours(5),
                expiredAt = LocalDateTime.now().plusDays(1)
            )
        )
    }

    private fun computeHash(questionId: Long, memberId: Long, choice: String, salt: String): String {
        val data = "$questionId:$memberId:$choice:$salt"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
