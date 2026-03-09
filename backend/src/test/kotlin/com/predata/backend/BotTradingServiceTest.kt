package com.predata.backend

import com.predata.backend.domain.Member
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.SwapAction
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.amm.SwapRequest
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.VoteCommitService
import com.predata.backend.service.amm.SwapService
import com.predata.backend.service.bot.BotMemberService
import com.predata.backend.service.bot.BotTradingService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.ZoneOffset

class BotTradingServiceTest {
    @Test
    fun `executeBotVoting does nothing when no bots`() {
        val botMemberService = mock<BotMemberService>()
        val voteCommitService = mock<VoteCommitService>()
        val swapService = mock<SwapService>()
        val questionRepository = mock<QuestionRepository>()
        whenever(botMemberService.getBotMembers()).thenReturn(emptyList())

        val service = BotTradingService(botMemberService, voteCommitService, swapService, questionRepository)
        service.executeBotVoting()

        verify(questionRepository, never()).findByStatus(QuestionStatus.VOTING)
        verify(voteCommitService, never()).commit(any(), any())
        verify(voteCommitService, never()).reveal(any(), any())
    }

    @Test
    fun `executeBotVoting commits in commit phase and reveals in reveal phase`() {
        val botMemberService = mock<BotMemberService>()
        val voteCommitService = mock<VoteCommitService>()
        val swapService = mock<SwapService>()
        val questionRepository = mock<QuestionRepository>()
        whenever(botMemberService.getBotMembers()).thenReturn(listOf(bot(1L)))
        whenever(questionRepository.findByStatus(QuestionStatus.VOTING)).thenReturn(
            listOf(votingQuestion(10L, VotingPhase.VOTING_COMMIT_OPEN)),
            listOf(votingQuestion(10L, VotingPhase.VOTING_REVEAL_OPEN))
        )

        val service = BotTradingService(botMemberService, voteCommitService, swapService, questionRepository)
        service.executeBotVoting()
        service.executeBotVoting()

        verify(voteCommitService, times(1)).commit(
            any(),
            argThat<VoteCommitRequest> { questionId == 10L && commitHash.isNotBlank() }
        )
        verify(voteCommitService, times(1)).reveal(
            any(),
            argThat<VoteRevealRequest> { questionId == 10L && salt.isNotBlank() }
        )
    }

    @Test
    fun `executeBotTrading executes buy swap for betting questions`() {
        val botMemberService = mock<BotMemberService>()
        val voteCommitService = mock<VoteCommitService>()
        val swapService = mock<SwapService>()
        val questionRepository = mock<QuestionRepository>()
        whenever(botMemberService.getBotMembers()).thenReturn(listOf(bot(2L)))
        whenever(questionRepository.findByStatus(QuestionStatus.BETTING)).thenReturn(
            listOf(bettingQuestion(20L))
        )

        val service = BotTradingService(botMemberService, voteCommitService, swapService, questionRepository)
        service.executeBotTrading()

        verify(swapService, times(1)).executeSwap(
            any(),
            argThat<SwapRequest> {
                questionId == 20L &&
                    action == SwapAction.BUY &&
                    usdcIn != null
            }
        )
    }

    private fun bot(id: Long): Member =
        Member(
            id = id,
            email = "bot_${id}@predata.bot",
            countryCode = "KR",
            role = "BOT",
            createdAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(10)
        )

    private fun votingQuestion(id: Long, phase: VotingPhase): Question =
        Question(
            id = id,
            title = "vote question $id",
            status = QuestionStatus.VOTING,
            votingPhase = phase,
            votingEndAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(1),
            bettingStartAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(2),
            bettingEndAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(3),
            expiredAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(4)
        )

    private fun bettingQuestion(id: Long): Question =
        Question(
            id = id,
            title = "bet question $id",
            status = QuestionStatus.BETTING,
            votingEndAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(2),
            bettingStartAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1),
            bettingEndAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(3),
            expiredAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(4)
        )
}
