package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.exception.DailyLimitExceededException
import com.predata.backend.repository.*
import com.predata.backend.service.VoteCommandService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * VOTE-028: 일일 한도 경합 테스트 (50스레드)
 *
 * 검증 목표:
 * - 동일 회원, 서로 다른 질문 50개에 50동시 투표
 * - 일일 한도 5 → 정확히 5개만 성공, 45개 실패
 * - daily_vote_usage.used_count = 5
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteDailyLimitConcurrencyTest {

    @Autowired lateinit var voteCommandService: VoteCommandService
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var activityRepository: ActivityRepository
    @Autowired lateinit var dailyVoteUsageRepository: DailyVoteUsageRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository
    @Autowired lateinit var voteRecordRepository: VoteRecordRepository

    private val threadCount = 50
    private val dailyLimit = 5
    private lateinit var member: Member
    private val questions = mutableListOf<Question>()

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(
            Member(email = "concurrency-limit-${UUID.randomUUID()}@test.com", countryCode = "KR")
        )
        repeat(threadCount) {
            questions.add(questionRepository.save(votableQuestion()))
        }
    }

    @AfterEach
    fun tearDown() {
        val memberId = member.id!!
        val questionIds = questions.mapNotNull { it.id }.toSet()

        onChainVoteRelayRepository.findAll()
            .filter { it.memberId == memberId }
            .forEach { onChainVoteRelayRepository.delete(it) }

        voteRecordRepository.findByMemberId(memberId)
            .forEach { voteRecordRepository.delete(it) }

        activityRepository.findByMemberId(memberId)
            .forEach { activityRepository.delete(it) }

        questionIds.forEach { qid ->
            voteSummaryRepository.findById(qid).ifPresent { voteSummaryRepository.delete(it) }
        }

        dailyVoteUsageRepository
            .findByMemberIdAndUsageDate(memberId, LocalDate.now(ZoneOffset.UTC))
            ?.let { dailyVoteUsageRepository.delete(it) }

        questions.forEach { questionRepository.delete(it) }
        memberRepository.delete(member)
        questions.clear()
    }

    @Test
    fun `50동시_서로다른질문_5개성공_45개실패_used_count=5`() {
        val memberId = member.id!!
        val successCount = AtomicInteger(0)
        val limitCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        questions.forEachIndexed { idx, question ->
            executor.submit {
                try {
                    voteCommandService.vote(
                        memberId = memberId,
                        questionId = question.id!!,
                        choice = if (idx % 2 == 0) Choice.YES else Choice.NO,
                    )
                    successCount.incrementAndGet()
                } catch (e: DailyLimitExceededException) {
                    limitCount.incrementAndGet()
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

        // 일일 한도 5 → 정확히 5개 성공
        assertThat(successCount.get()).isEqualTo(dailyLimit)
        assertThat(limitCount.get()).isEqualTo(threadCount - dailyLimit)

        // used_count = 5
        val usage = dailyVoteUsageRepository.findByMemberIdAndUsageDate(
            memberId, LocalDate.now(ZoneOffset.UTC)
        )
        assertThat(usage?.usedCount).isEqualTo(dailyLimit)

        println(
            "[VOTE-028] 성공: ${successCount.get()}, " +
            "한도초과: ${limitCount.get()}, 오류: ${errorCount.get()}"
        )
    }

    private fun votableQuestion(): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return Question(
            title = "한도테스트 ${UUID.randomUUID()}",
            status = QuestionStatus.VOTING,
            votingEndAt = now.plusHours(1),
            bettingStartAt = now.plusHours(2),
            bettingEndAt = now.plusHours(3),
            expiredAt = now.plusHours(4),
        )
    }
}
