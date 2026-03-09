package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.exception.AlreadyVotedException
import com.predata.backend.exception.DailyLimitExceededException
import com.predata.backend.repository.*
import com.predata.backend.service.VoteCommandService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * VOTE-031: Redis 장애 fallback 테스트
 *
 * Redis가 완전히 다운된 환경을 시뮬레이션하고
 * VoteCommandService가 DB 경로만으로 올바르게 동작하는지 검증한다.
 *
 * 검증 목표:
 * 1. Redis 장애 시에도 투표 성공 (DB 기반)
 * 2. 일일 한도 차단이 DB 기반으로 정상 작동
 * 3. 중복 투표 차단이 DB 기반으로 정상 작동
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteRedisFallbackTest {

    /** Redis를 완전히 장애 상태로 시뮬레이션 */
    @MockBean
    lateinit var stringRedisTemplate: StringRedisTemplate

    @Autowired lateinit var voteCommandService: VoteCommandService
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var activityRepository: ActivityRepository
    @Autowired lateinit var dailyVoteUsageRepository: DailyVoteUsageRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository
    @Autowired lateinit var voteRecordRepository: VoteRecordRepository

    private lateinit var member: Member
    private val questions = mutableListOf<Question>()

    @BeforeEach
    fun setUp() {
        // Redis 모든 연산 → RedisConnectionFailureException 발생 (장애 시뮬레이션)
        val valueOps = Mockito.mock(ValueOperations::class.java)
        Mockito.`when`(stringRedisTemplate.opsForValue()).thenReturn(valueOps as ValueOperations<String, String>)
        Mockito.`when`(valueOps.get(Mockito.anyString()))
            .thenThrow(RedisConnectionFailureException("Redis is down"))
        Mockito.`when`(valueOps.set(Mockito.anyString(), Mockito.anyString()))
            .thenThrow(RedisConnectionFailureException("Redis is down"))
        Mockito.`when`(valueOps.increment(Mockito.anyString()))
            .thenThrow(RedisConnectionFailureException("Redis is down"))

        member = memberRepository.save(
            Member(email = "redis-fallback-${UUID.randomUUID()}@test.com", countryCode = "KR")
        )
    }

    @AfterEach
    fun tearDown() {
        val memberId = member.id!!

        onChainVoteRelayRepository.findAll()
            .filter { it.memberId == memberId }
            .forEach { onChainVoteRelayRepository.delete(it) }

        voteRecordRepository.findByMemberId(memberId)
            .forEach { voteRecordRepository.delete(it) }

        activityRepository.findByMemberId(memberId)
            .forEach { activityRepository.delete(it) }

        questions.forEach { q ->
            q.id?.let { qid ->
                voteSummaryRepository.findById(qid).ifPresent { voteSummaryRepository.delete(it) }
            }
        }

        dailyVoteUsageRepository
            .findByMemberIdAndUsageDate(member.id!!, LocalDate.now(ZoneOffset.UTC))
            ?.let { dailyVoteUsageRepository.delete(it) }

        questions.forEach { questionRepository.delete(it) }
        memberRepository.delete(member)
        questions.clear()
    }

    @Test
    fun `Redis 장애 시에도 투표 성공 (DB 기반)`() {
        val question = saveQuestion()
        val memberId = member.id!!

        // VoteCommandService는 Redis 미의존 → 성공
        val activity = voteCommandService.vote(memberId, question.id!!, Choice.YES)
        assertThat(activity.id).isNotNull()
        assertThat(activity.activityType).isEqualTo(ActivityType.VOTE)

        // daily_vote_usage DB 레코드 생성 확인
        val usage = dailyVoteUsageRepository.findByMemberIdAndUsageDate(
            memberId, LocalDate.now(ZoneOffset.UTC)
        )
        assertThat(usage?.usedCount).isEqualTo(1)
    }

    @Test
    fun `Redis 장애 시 일일 한도 차단이 DB 기반으로 작동`() {
        val dailyLimit = 5
        val memberId = member.id!!

        // 5개 다른 질문에 투표 (한도 채우기)
        repeat(dailyLimit) {
            val q = saveQuestion()
            voteCommandService.vote(memberId, q.id!!, Choice.YES)
        }

        // 6번째 투표는 DailyLimitExceededException
        val extraQuestion = saveQuestion()
        assertThatThrownBy {
            voteCommandService.vote(memberId, extraQuestion.id!!, Choice.NO)
        }.isInstanceOf(DailyLimitExceededException::class.java)

        // used_count = 5 (DB 기록 정확)
        val usage = dailyVoteUsageRepository.findByMemberIdAndUsageDate(
            memberId, LocalDate.now(ZoneOffset.UTC)
        )
        assertThat(usage?.usedCount).isEqualTo(dailyLimit)
    }

    @Test
    fun `Redis 장애 시 중복 투표 차단이 DB 기반으로 작동`() {
        val question = saveQuestion()
        val memberId = member.id!!

        // 첫 번째 투표 성공
        voteCommandService.vote(memberId, question.id!!, Choice.YES)

        // 두 번째 투표 → AlreadyVotedException
        assertThatThrownBy {
            voteCommandService.vote(memberId, question.id!!, Choice.YES)
        }.isInstanceOf(AlreadyVotedException::class.java)

        // activity는 1개만 존재
        val activities = activityRepository.findByMemberId(memberId)
            .filter { it.activityType == ActivityType.VOTE && it.questionId == question.id }
        assertThat(activities).hasSize(1)
    }

    private fun saveQuestion(): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val q = questionRepository.save(
            Question(
                title = "fallback-test ${UUID.randomUUID()}",
                status = QuestionStatus.VOTING,
                votingEndAt = now.plusHours(1),
                bettingStartAt = now.plusHours(2),
                bettingEndAt = now.plusHours(3),
                expiredAt = now.plusHours(4),
            )
        )
        questions.add(q)
        return q
    }
}
