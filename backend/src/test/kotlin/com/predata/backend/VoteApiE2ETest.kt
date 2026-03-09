package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.repository.*
import com.predata.backend.service.VoteCommandService
import com.predata.backend.service.VoteQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * VOTE-027: 투표 정상 E2E 검증
 *
 * 검증 목표:
 * 1. vote() 성공 시 Activity 생성
 * 2. remainingVotes = dailyLimit - 1
 * 3. VoteSummary yesCount 또는 noCount 증가
 * 4. OnChainVoteRelay PENDING 생성
 */
@SpringBootTest
@ActiveProfiles("test")
class VoteApiE2ETest {

    @Autowired lateinit var voteCommandService: VoteCommandService
    @Autowired lateinit var voteQueryService: VoteQueryService
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var activityRepository: ActivityRepository
    @Autowired lateinit var dailyVoteUsageRepository: DailyVoteUsageRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository
    @Autowired lateinit var voteRecordRepository: VoteRecordRepository

    private lateinit var member: Member
    private lateinit var question: Question

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(
            Member(email = "e2e-${UUID.randomUUID()}@test.com", countryCode = "KR")
        )
        question = questionRepository.save(votableQuestion())
    }

    @AfterEach
    fun tearDown() {
        onChainVoteRelayRepository.findAll()
            .filter { it.memberId == member.id }
            .forEach { onChainVoteRelayRepository.delete(it) }

        voteRecordRepository.findByMemberId(member.id!!)
            .forEach { voteRecordRepository.delete(it) }

        activityRepository.findByMemberId(member.id!!)
            .forEach { activityRepository.delete(it) }

        voteSummaryRepository.findById(question.id!!).ifPresent { voteSummaryRepository.delete(it) }
        dailyVoteUsageRepository.findByMemberIdAndUsageDate(member.id!!, java.time.LocalDate.now(ZoneOffset.UTC))
            ?.let { dailyVoteUsageRepository.delete(it) }

        questionRepository.delete(question)
        memberRepository.delete(member)
    }

    @Test
    fun `정상 투표 - Activity 생성, remaining 감소, summary 증가, relay PENDING 생성`() {
        val memberId = member.id!!
        val questionId = question.id!!

        // 투표 전 상태
        val remainingBefore = voteQueryService.remainingDailyVotes(memberId)
        assertThat(remainingBefore).isEqualTo(5)

        // 투표 실행
        val activity = voteCommandService.vote(memberId, questionId, Choice.YES, "127.0.0.1")

        // 1. Activity 생성 확인
        assertThat(activity.id).isNotNull()
        assertThat(activity.memberId).isEqualTo(memberId)
        assertThat(activity.questionId).isEqualTo(questionId)
        assertThat(activity.activityType).isEqualTo(ActivityType.VOTE)
        assertThat(activity.choice).isEqualTo(Choice.YES)

        // 2. remainingVotes 감소
        val remainingAfter = voteQueryService.remainingDailyVotes(memberId)
        assertThat(remainingAfter).isEqualTo(4)

        // 3. VoteSummary 증가
        val summary = voteSummaryRepository.findById(questionId).orElseThrow()
        assertThat(summary.yesCount).isEqualTo(1)
        assertThat(summary.noCount).isEqualTo(0)
        assertThat(summary.totalCount).isEqualTo(1)

        // 4. OnChainVoteRelay PENDING 생성
        val relays = onChainVoteRelayRepository.findByStatusOrderByCreatedAtAsc(OnChainRelayStatus.PENDING)
            .filter { it.memberId == memberId && it.questionId == questionId }
        assertThat(relays).hasSize(1)
        assertThat(relays.first().txHash).isNull()
        assertThat(relays.first().choice).isEqualTo(Choice.YES)
    }

    @Test
    fun `alreadyVoted - 투표 후 canVote 조회 시 ALREADY_VOTED 반환`() {
        val memberId = member.id!!
        val questionId = question.id!!

        voteCommandService.vote(memberId, questionId, Choice.NO)

        val status = voteQueryService.canVote(memberId, questionId)
        assertThat(status.canVote).isFalse()
        assertThat(status.alreadyVoted).isTrue()
        assertThat(status.reason).isEqualTo("ALREADY_VOTED")
    }

    private fun votableQuestion(): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return Question(
            title = "E2E 테스트 질문 ${UUID.randomUUID()}",
            status = QuestionStatus.VOTING,
            votingEndAt = now.plusHours(1),
            bettingStartAt = now.plusHours(2),
            bettingEndAt = now.plusHours(3),
            expiredAt = now.plusHours(4),
        )
    }
}
