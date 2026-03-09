package com.predata.backend.question

import com.predata.backend.domain.DraftStatus
import com.predata.backend.domain.Member
import com.predata.backend.domain.QuestionCreditAccount
import com.predata.backend.domain.SettlementMode
import com.predata.backend.domain.VoteWindowType
import com.predata.backend.dto.SubmitQuestionDraftRequest
import com.predata.backend.exception.BadRequestException
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class QuestionSplitValidationTest {

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
    fun `creatorSplitInPool must be 10-step`() {
        val member = createMemberAndCredit()
        val draft = createDraft(member.id!!)

        val request = SubmitQuestionDraftRequest(
            title = "분배 비율 검증 실패 ${UUID.randomUUID()}",
            category = "GENERAL",
            voteWindowType = VoteWindowType.H6,
            settlementMode = SettlementMode.VOTE_RESULT,
            creatorSplitInPool = 15,
        )

        val thrown = runCatching {
            questionDraftService.submitDraft(member.id!!, draft.draftId, draft.submitIdempotencyKey, request)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun `valid creatorSplitInPool persists normalized fee shares`() {
        val member = createMemberAndCredit()
        val draft = createDraft(member.id!!)

        val request = SubmitQuestionDraftRequest(
            title = "분배 비율 검증 성공 ${UUID.randomUUID()}",
            category = "GENERAL",
            voteWindowType = VoteWindowType.H6,
            settlementMode = SettlementMode.VOTE_RESULT,
            creatorSplitInPool = 30, // 80% * 30% = 24%
        )

        val response = questionDraftService.submitDraft(member.id!!, draft.draftId, draft.submitIdempotencyKey, request)
        val question = questionRepository.findById(response.questionId).orElseThrow()

        assertThat(question.platformFeeShare).isEqualByComparingTo(BigDecimal("0.2000"))
        assertThat(question.creatorFeeShare).isEqualByComparingTo(BigDecimal("0.2400"))
        assertThat(question.voterFeeShare).isEqualByComparingTo(BigDecimal("0.5600"))
        assertThat(question.creatorSplitInPool).isEqualTo(30)
    }

    private fun createMemberAndCredit(): Member {
        val member = memberRepository.save(
            Member(
                email = "split-${UUID.randomUUID()}@test.com",
                countryCode = "KR",
                role = "USER",
            )
        )
        creditAccountRepository.save(
            QuestionCreditAccount(
                memberId = member.id!!,
                availableCredits = 365,
                yearlyBudget = 365,
                lastResetAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        )
        return member
    }

    private fun createDraft(memberId: Long) =
        draftSessionRepository.save(
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
}
