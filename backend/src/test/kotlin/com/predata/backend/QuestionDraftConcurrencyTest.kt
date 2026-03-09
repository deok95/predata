package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.dto.SubmitQuestionDraftRequest
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.RateLimitException
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionCreditAccountRepository
import com.predata.backend.repository.QuestionCreditLedgerRepository
import com.predata.backend.repository.QuestionDraftSessionRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.QuestionDraftService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 크레딧 기반 질문 생성 동시성 테스트
 *
 * 검증 목표:
 * 1. 50개 스레드에서 동일 draftId로 submit 호출 시 크레딧 1개만 차감
 * 2. 질문 1개만 생성 (중복 생성 방지)
 * 3. 모든 스레드가 정상 또는 Conflict로 완료 (데드락/무한대기 없음)
 *
 * Redis는 @MockBean으로 대체 — DB 레벨 동시성만 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class QuestionDraftConcurrencyTest {

    @MockBean
    lateinit var stringRedisTemplate: StringRedisTemplate

    @Autowired
    lateinit var questionDraftService: QuestionDraftService

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var creditAccountRepository: QuestionCreditAccountRepository

    @Autowired
    lateinit var creditLedgerRepository: QuestionCreditLedgerRepository

    @Autowired
    lateinit var draftSessionRepository: QuestionDraftSessionRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    private lateinit var testMember: Member
    private lateinit var testDraft: QuestionDraftSession
    private val initialCredits = 10

    @BeforeEach
    fun setUp() {
        // Redis mock: 모든 get → null (DB fallback 유도), set/delete → no-op
        val valueOps = Mockito.mock(ValueOperations::class.java)
        Mockito.`when`(stringRedisTemplate.opsForValue()).thenReturn(valueOps as ValueOperations<String, String>)
        Mockito.`when`(valueOps.get(Mockito.anyString())).thenReturn(null)
        Mockito.`when`(valueOps.increment(Mockito.anyString())).thenReturn(0L) // rate-limit 스킵

        testMember = memberRepository.save(
            Member(
                email = "concurrency-test-${UUID.randomUUID()}@test.com",
                countryCode = "KR",
                role = "USER",
            )
        )

        creditAccountRepository.save(
            QuestionCreditAccount(
                memberId = testMember.id!!,
                availableCredits = initialCredits,
            )
        )

        val now = LocalDateTime.now(ZoneOffset.UTC)
        testDraft = draftSessionRepository.save(
            QuestionDraftSession(
                draftId = UUID.randomUUID().toString(),
                memberId = testMember.id!!,
                status = DraftStatus.OPEN,
                expiresAt = now.plusMinutes(30),
                submitIdempotencyKey = UUID.randomUUID().toString(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    @AfterEach
    fun tearDown() {
        // 생성된 질문 → 원장 → draft → credit account → member 순으로 정리
        questionRepository.findAll()
            .filter { it.creatorMemberId == testMember.id }
            .forEach { questionRepository.delete(it) }

        creditLedgerRepository.findAll()
            .filter { it.memberId == testMember.id }
            .forEach { creditLedgerRepository.delete(it) }

        draftSessionRepository.findByDraftId(testDraft.draftId)
            ?.let { draftSessionRepository.delete(it) }
        creditAccountRepository.findByMemberId(testMember.id!!)
            ?.let { creditAccountRepository.delete(it) }
        memberRepository.delete(testMember)
    }

    @Test
    fun `50개 스레드 동일 draftId 동시 submit시 크레딧 1개만 차감되고 질문 1개만 생성된다`() {
        val threadCount = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val conflictCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)

        val request = SubmitQuestionDraftRequest(
            title = "동시성 테스트 질문 ${UUID.randomUUID()}",
            category = "GENERAL",
            voteWindowType = VoteWindowType.H6,
            settlementMode = SettlementMode.VOTE_RESULT,
        )

        repeat(threadCount) {
            executor.submit {
                try {
                    questionDraftService.submitDraft(
                        memberId = testMember.id!!,
                        draftId = testDraft.draftId,
                        idempotencyKey = testDraft.submitIdempotencyKey,
                        request = request,
                    )
                    successCount.incrementAndGet()
                } catch (e: ConflictException) {
                    conflictCount.incrementAndGet()
                } catch (e: RateLimitException) {
                    conflictCount.incrementAndGet()
                } catch (e: ObjectOptimisticLockingFailureException) {
                    conflictCount.incrementAndGet()
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(completed).isTrue()
        assertThat(errorCount.get()).isEqualTo(0)

        // 크레딧 1개만 차감
        val finalAccount = creditAccountRepository.findByMemberId(testMember.id!!)!!
        assertThat(finalAccount.availableCredits).isEqualTo(initialCredits - 1)

        // 질문 1개만 생성
        val createdQuestions = questionRepository.findAll()
            .filter { it.creatorMemberId == testMember.id }
        assertThat(createdQuestions).hasSize(1)

        // 원장 1건만 기록
        val ledgers = creditLedgerRepository.findAll()
            .filter { it.memberId == testMember.id && it.reason == CreditLedgerReason.QUESTION_CREATED }
        assertThat(ledgers).hasSize(1)
        assertThat(ledgers.first().delta).isEqualTo(-1)
        assertThat(ledgers.first().balanceAfter).isEqualTo(initialCredits - 1)

        // draft 상태 CONSUMED
        val finalDraft = draftSessionRepository.findByDraftId(testDraft.draftId)!!
        assertThat(finalDraft.status).isEqualTo(DraftStatus.CONSUMED)
        assertThat(finalDraft.submittedQuestionId).isNotNull()

        println(
            "[결과] 성공: ${successCount.get()}, " +
                "충돌/락타임아웃: ${conflictCount.get()}, " +
                "오류: ${errorCount.get()}"
        )
    }
}
