package com.predata.backend.question

import com.predata.backend.domain.DraftStatus
import com.predata.backend.domain.Member
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionCreditAccount
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.SettlementMode
import com.predata.backend.domain.VoteWindowType
import com.predata.backend.dto.SubmitQuestionDraftRequest
import com.predata.backend.exception.ConflictException
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class QuestionQuotaPolicyTest {

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
    fun `same member second create in same UTC day is blocked`() {
        val member = createMember("daily-limit")
        createCreditAccount(member.id!!)

        val request = SubmitQuestionDraftRequest(
            title = "하루 생성 제한 테스트 ${UUID.randomUUID()}",
            category = "GENERAL",
            voteWindowType = VoteWindowType.H6,
            settlementMode = SettlementMode.VOTE_RESULT,
        )

        val firstDraft = createDraft(member.id!!)
        questionDraftService.submitDraft(member.id!!, firstDraft.draftId, firstDraft.submitIdempotencyKey, request)

        val secondDraft = createDraft(member.id!!)
        val thrown = runCatching {
            questionDraftService.submitDraft(member.id!!, secondDraft.draftId, secondDraft.submitIdempotencyKey, request)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(ConflictException::class.java)
        assertThat((thrown as ConflictException).code).isEqualTo("DAILY_CREATE_LIMIT_EXCEEDED")
    }

    @Test
    fun `active question lock blocks creation even if daily count is from previous day`() {
        val member = createMember("active-lock")
        createCreditAccount(member.id!!)

        val now = LocalDateTime.now(ZoneOffset.UTC)
        questionRepository.save(
            Question(
                title = "기존 활성 질문",
                category = "GENERAL",
                status = QuestionStatus.VOTING,
                votingEndAt = now.plusHours(3),
                bettingStartAt = now.plusHours(3).plusMinutes(5),
                bettingEndAt = now.plusDays(1),
                expiredAt = now.plusDays(1),
                creatorMemberId = member.id,
                createdAt = now.minusDays(1), // 하루 제한은 회피되지만 active lock은 유지되어야 함
            )
        )

        val request = SubmitQuestionDraftRequest(
            title = "활성 잠금 테스트 ${UUID.randomUUID()}",
            category = "GENERAL",
            voteWindowType = VoteWindowType.H6,
            settlementMode = SettlementMode.VOTE_RESULT,
        )
        val draft = createDraft(member.id!!)
        val thrown = runCatching {
            questionDraftService.submitDraft(member.id!!, draft.draftId, draft.submitIdempotencyKey, request)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(ConflictException::class.java)
        assertThat((thrown as ConflictException).code).isEqualTo("ACTIVE_QUESTION_EXISTS")
    }

    private fun createMember(prefix: String): Member =
        memberRepository.save(
            Member(
                email = "$prefix-${UUID.randomUUID()}@test.com",
                countryCode = "KR",
                role = "USER",
            )
        )

    private fun createCreditAccount(memberId: Long) {
        creditAccountRepository.save(
            QuestionCreditAccount(
                memberId = memberId,
                availableCredits = 365,
                yearlyBudget = 365,
                lastResetAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        )
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
