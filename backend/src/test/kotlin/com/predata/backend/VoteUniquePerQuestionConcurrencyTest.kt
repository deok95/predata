package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.exception.AlreadyVotedException
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
 * VOTE-029: 질문당 1회 경합 테스트
 *
 * 검증 목표:
 * - 동일 회원, 동일 질문에 20동시 투표
 * - 정확히 1개만 성공, 19개 실패
 * - vote_summary.total_count = 1
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteUniquePerQuestionConcurrencyTest {

    @Autowired lateinit var voteCommandService: VoteCommandService
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var activityRepository: ActivityRepository
    @Autowired lateinit var dailyVoteUsageRepository: DailyVoteUsageRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository
    @Autowired lateinit var voteRecordRepository: VoteRecordRepository

    private val threadCount = 20
    private lateinit var member: Member
    private lateinit var question: Question

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(
            Member(email = "concurrency-unique-${UUID.randomUUID()}@test.com", countryCode = "KR")
        )
        question = questionRepository.save(votableQuestion())
    }

    @AfterEach
    fun tearDown() {
        val memberId = member.id!!
        val questionId = question.id!!

        onChainVoteRelayRepository.findAll()
            .filter { it.memberId == memberId && it.questionId == questionId }
            .forEach { onChainVoteRelayRepository.delete(it) }

        voteRecordRepository.findByMemberId(memberId)
            .forEach { voteRecordRepository.delete(it) }

        activityRepository.findByMemberId(memberId)
            .forEach { activityRepository.delete(it) }

        voteSummaryRepository.findById(questionId).ifPresent { voteSummaryRepository.delete(it) }

        dailyVoteUsageRepository
            .findByMemberIdAndUsageDate(memberId, LocalDate.now(ZoneOffset.UTC))
            ?.let { dailyVoteUsageRepository.delete(it) }

        questionRepository.delete(question)
        memberRepository.delete(member)
    }

    @Test
    fun `동일회원_동일질문_20동시_1개성공_19개실패`() {
        val memberId = member.id!!
        val questionId = question.id!!
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) {
            executor.submit {
                try {
                    voteCommandService.vote(memberId, questionId, Choice.YES)
                    successCount.incrementAndGet()
                } catch (e: AlreadyVotedException) {
                    // 선행 exists 체크 또는 DB unique 위반 모두 AlreadyVotedException으로 변환됨
                    failCount.incrementAndGet()
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

        // 정확히 1개 성공, 19개 실패
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(threadCount - 1)

        // vote_summary.total_count = 1
        val summary = voteSummaryRepository.findById(questionId).orElseThrow()
        assertThat(summary.totalCount).isEqualTo(1)
        assertThat(summary.yesCount).isEqualTo(1)

        println(
            "[VOTE-029] 성공: ${successCount.get()}, " +
            "실패: ${failCount.get()}, 오류: ${errorCount.get()}"
        )
    }

    private fun votableQuestion(): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return Question(
            title = "중복테스트 ${UUID.randomUUID()}",
            status = QuestionStatus.VOTING,
            votingEndAt = now.plusHours(1),
            bettingStartAt = now.plusHours(2),
            bettingEndAt = now.plusHours(3),
            expiredAt = now.plusHours(4),
        )
    }
}
