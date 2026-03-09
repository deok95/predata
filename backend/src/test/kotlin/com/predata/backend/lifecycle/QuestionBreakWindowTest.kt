package com.predata.backend.lifecycle

import com.predata.backend.domain.DraftStatus
import com.predata.backend.domain.Member
import com.predata.backend.domain.QuestionCreditAccount
import com.predata.backend.domain.SettlementMode
import com.predata.backend.domain.VoteWindowType
import com.predata.backend.dto.SubmitQuestionDraftRequest
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionCreditAccountRepository
import com.predata.backend.repository.QuestionDraftSessionRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.QuestionDraftService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class QuestionBreakWindowTest {

    @MockBean
    lateinit var stringRedisTemplate: StringRedisTemplate

    @Autowired
    lateinit var questionDraftService: QuestionDraftService

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var creditAccountRepository: QuestionCreditAccountRepository

    @Autowired
    lateinit var draftSessionRepository: QuestionDraftSessionRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @BeforeEach
    fun setUpRedisMock() {
        val valueOps = Mockito.mock(ValueOperations::class.java)
        Mockito.`when`(stringRedisTemplate.opsForValue()).thenReturn(valueOps as ValueOperations<String, String>)
        Mockito.`when`(valueOps.get(Mockito.anyString())).thenReturn(null)
        Mockito.`when`(valueOps.increment(Mockito.anyString())).thenReturn(0L)
    }

    @Test
    fun `H6 maps to break 5m and betting 1d`() {
        val question = createQuestion(VoteWindowType.H6)
        // VOTE_RESULT: break=5분, 베팅 기간=reveal 기간 (revealWindowEndAt=bettingEndAt)
        assertThat(Duration.between(question.votingEndAt, question.bettingStartAt)).isEqualTo(Duration.ofMinutes(5))
        assertThat(Duration.between(question.bettingStartAt, question.bettingEndAt)).isEqualTo(Duration.ofDays(1))
        assertThat(question.revealWindowEndAt).isEqualTo(question.bettingEndAt)
    }

    @Test
    fun `D1 maps to break 5m and betting 3d`() {
        val question = createQuestion(VoteWindowType.D1)
        assertThat(Duration.between(question.votingEndAt, question.bettingStartAt)).isEqualTo(Duration.ofMinutes(5))
        assertThat(Duration.between(question.bettingStartAt, question.bettingEndAt)).isEqualTo(Duration.ofDays(3))
        assertThat(question.revealWindowEndAt).isEqualTo(question.bettingEndAt)
    }

    @Test
    fun `D3 maps to break 5m and betting 7d`() {
        val question = createQuestion(VoteWindowType.D3)
        assertThat(Duration.between(question.votingEndAt, question.bettingStartAt)).isEqualTo(Duration.ofMinutes(5))
        assertThat(Duration.between(question.bettingStartAt, question.bettingEndAt)).isEqualTo(Duration.ofDays(7))
        assertThat(question.revealWindowEndAt).isEqualTo(question.bettingEndAt)
    }

    private fun createQuestion(voteWindowType: VoteWindowType): com.predata.backend.domain.Question {
        val member = memberRepository.save(
            Member(
                email = "break-${voteWindowType.name}-${UUID.randomUUID()}@test.com",
                countryCode = "KR",
                role = "USER",
            )
        )
        val memberId = member.id!!
        creditAccountRepository.save(
            QuestionCreditAccount(
                memberId = memberId,
                availableCredits = 365,
                yearlyBudget = 365,
                lastResetAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        )
        val draft = draftSessionRepository.save(
            com.predata.backend.domain.QuestionDraftSession(
                draftId = UUID.randomUUID().toString(),
                memberId = memberId,
                activeMemberId = memberId,
                status = DraftStatus.OPEN,
                expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30),
                submitIdempotencyKey = UUID.randomUUID().toString(),
                createdAt = LocalDateTime.now(ZoneOffset.UTC),
                updatedAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        )
        val request = SubmitQuestionDraftRequest(
            title = "브레이크/베팅 매핑 ${voteWindowType.name} ${UUID.randomUUID()}",
            category = "GENERAL",
            voteWindowType = voteWindowType,
            settlementMode = SettlementMode.VOTE_RESULT,
            creatorSplitInPool = 50,
        )
        val response = questionDraftService.submitDraft(memberId, draft.draftId, draft.submitIdempotencyKey, request)
        return questionRepository.findById(response.questionId).orElseThrow()
    }
}
