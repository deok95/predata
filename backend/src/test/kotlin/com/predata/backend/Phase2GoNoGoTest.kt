package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.exception.AlreadyVotedException
import com.predata.backend.exception.DailyLimitExceededException
import com.predata.backend.repository.*
import com.predata.backend.service.VoteCommandService
import com.predata.backend.service.VoteQueryService
import com.predata.backend.service.relay.RelayProcessor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Phase 2 Go/No-Go 수용 기준 검증
 *
 * GNG-01: POST /api/votes 동작 — 5번째 투표 성공, 6번째 DAILY_LIMIT_EXCEEDED (429)
 * GNG-02: 동일 질문 재투표 — ALREADY_VOTED (409)
 * GNG-03: vote_summary & vote_records 정합성 일치
 * GNG-04: relay PENDING → CONFIRMED 전환 (mock 기준)
 * GNG-05: GET /api/votes/status — canVote/remainingVotes 정확성
 * GNG-06: 운영 API VoteOpsAdminController 빈 등록 확인
 *
 * 전체 기준 통과 시 "Phase 2 Go" 선언.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName::class)
class Phase2GoNoGoTest {

    @Autowired lateinit var voteCommandService: VoteCommandService
    @Autowired lateinit var voteQueryService: VoteQueryService
    @Autowired lateinit var relayProcessor: RelayProcessor
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var activityRepository: ActivityRepository
    @Autowired lateinit var dailyVoteUsageRepository: DailyVoteUsageRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository
    @Autowired lateinit var voteRecordRepository: VoteRecordRepository

    // ApplicationContext 빈 존재 확인용
    @Autowired
    lateinit var appContext: org.springframework.context.ApplicationContext

    private lateinit var member: Member
    private val questions = mutableListOf<Question>()

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(
            Member(email = "gng-${UUID.randomUUID()}@test.com", countryCode = "KR")
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

    // ─────────────────────────────────────────────────────────────────────────
    // GNG-01: 일일 한도 (5 성공, 6번째 DAILY_LIMIT_EXCEEDED → 429)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-01 일일한도 5번째 성공 6번째 429 DAILY_LIMIT_EXCEEDED`() {
        val memberId = member.id!!
        val dailyLimit = 5

        repeat(dailyLimit) {
            val q = saveQuestion()
            val activity = voteCommandService.vote(memberId, q.id!!, Choice.YES)
            assertThat(activity.id).isNotNull()
        }

        val usage = dailyVoteUsageRepository.findByMemberIdAndUsageDate(
            memberId, LocalDate.now(ZoneOffset.UTC)
        )
        assertThat(usage?.usedCount).isEqualTo(dailyLimit)

        val extraQ = saveQuestion()
        assertThatThrownBy {
            voteCommandService.vote(memberId, extraQ.id!!, Choice.NO)
        }.isInstanceOf(DailyLimitExceededException::class.java)
            .hasMessageContaining("일일 투표 한도")

        // used_count 는 5 그대로 (6번째 트랜잭션 롤백)
        val usageAfter = dailyVoteUsageRepository.findByMemberIdAndUsageDate(
            memberId, LocalDate.now(ZoneOffset.UTC)
        )
        assertThat(usageAfter?.usedCount).isEqualTo(dailyLimit)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GNG-02: 동일 질문 재투표 → AlreadyVotedException (409)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-02 동일질문 재투표 409 ALREADY_VOTED`() {
        val memberId = member.id!!
        val q = saveQuestion()

        voteCommandService.vote(memberId, q.id!!, Choice.YES)

        assertThatThrownBy {
            voteCommandService.vote(memberId, q.id!!, Choice.YES)
        }.isInstanceOf(AlreadyVotedException::class.java)
            .hasMessageContaining("이미 투표")

        // activity 는 1건만
        val activities = activityRepository.findByMemberId(memberId)
            .filter { it.activityType == ActivityType.VOTE }
        assertThat(activities).hasSize(1)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GNG-03: vote_summary vs vote_records 정합성
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-03 vote_summary와 vote_records 정합성`() {
        val q = saveQuestion()
        val questionId = q.id!!

        // 두 번째 회원 (YES 투표)
        val memberYes = memberRepository.save(
            Member(email = "gng-yes-${UUID.randomUUID()}@test.com", countryCode = "KR")
        )
        // 세 번째 회원 (NO 투표)
        val memberNo = memberRepository.save(
            Member(email = "gng-no-${UUID.randomUUID()}@test.com", countryCode = "KR")
        )

        try {
            voteCommandService.vote(member.id!!, questionId, Choice.YES)
            voteCommandService.vote(memberYes.id!!, questionId, Choice.YES)
            voteCommandService.vote(memberNo.id!!, questionId, Choice.NO)

            // vote_summary 확인
            val summary = voteSummaryRepository.findById(questionId).orElseThrow()
            assertThat(summary.yesCount).isEqualTo(2)
            assertThat(summary.noCount).isEqualTo(1)
            assertThat(summary.totalCount).isEqualTo(3)

            // vote_records 건수 확인 (정합성)
            val yesRecords = voteRecordRepository.countByQuestionIdAndChoice(questionId, Choice.YES)
            val noRecords = voteRecordRepository.countByQuestionIdAndChoice(questionId, Choice.NO)
            assertThat(yesRecords).isEqualTo(summary.yesCount)
            assertThat(noRecords).isEqualTo(summary.noCount)

        } finally {
            // cleanup extra members
            cleanup(memberYes.id!!, questionId)
            cleanup(memberNo.id!!, questionId)
            memberRepository.delete(memberYes)
            memberRepository.delete(memberNo)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GNG-04: relay PENDING → CONFIRMED (MockOnChainRelayService 기준)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-04 relay PENDING to CONFIRMED mock 전환`() {
        val memberId = member.id!!
        val q = saveQuestion()
        val questionId = q.id!!

        // 투표 → relay PENDING 생성
        voteCommandService.vote(memberId, questionId, Choice.YES)

        val relaysBefore = onChainVoteRelayRepository
            .findByStatusOrderByCreatedAtAsc(OnChainRelayStatus.PENDING)
            .filter { it.memberId == memberId && it.questionId == questionId }
        assertThat(relaysBefore).hasSize(1)
        assertThat(relaysBefore.first().txHash).isNull()

        // 스케줄러 processor 직접 실행 (MockOnChainRelayService → 즉시 CONFIRMED)
        relayProcessor.processBatch()

        val relayAfter = onChainVoteRelayRepository.findById(relaysBefore.first().id!!).orElseThrow()
        assertThat(relayAfter.status).isEqualTo(OnChainRelayStatus.CONFIRMED)
        assertThat(relayAfter.txHash).startsWith("mock_")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GNG-05: GET /api/votes/status → canVote & remainingVotes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-05 canVote 및 remainingVotes 정확성`() {
        val memberId = member.id!!
        val q = saveQuestion()

        // 투표 전
        val before = voteQueryService.canVote(memberId, q.id!!)
        assertThat(before.canVote).isTrue()
        assertThat(before.remainingDailyVotes).isEqualTo(5)
        assertThat(before.alreadyVoted).isFalse()

        voteCommandService.vote(memberId, q.id!!, Choice.YES)

        // 투표 후: 같은 질문 → ALREADY_VOTED
        val afterSame = voteQueryService.canVote(memberId, q.id!!)
        assertThat(afterSame.canVote).isFalse()
        assertThat(afterSame.alreadyVoted).isTrue()
        assertThat(afterSame.reason).isEqualTo("ALREADY_VOTED")

        // 다른 질문 → remaining = 4
        val q2 = saveQuestion()
        val afterOther = voteQueryService.canVote(memberId, q2.id!!)
        assertThat(afterOther.canVote).isTrue()
        assertThat(afterOther.remainingDailyVotes).isEqualTo(4)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GNG-06: 운영 API VoteOpsAdminController 빈 등록 확인
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-06 운영 API VoteOpsAdminController 빈 등록 확인`() {
        assertThat(
            appContext.containsBean("voteOpsAdminController")
        ).isTrue()

        // RelayAdminService 등록 확인
        assertThat(
            appContext.containsBean("relayAdminService")
        ).isTrue()

        // VoteDataRetentionScheduler 등록 확인
        assertThat(
            appContext.containsBean("voteDataRetentionScheduler")
        ).isTrue()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveQuestion(): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val q = questionRepository.save(
            Question(
                title = "GNG 질문 ${UUID.randomUUID()}",
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

    private fun cleanup(memberId: Long, questionId: Long) {
        onChainVoteRelayRepository.findAll()
            .filter { it.memberId == memberId }
            .forEach { onChainVoteRelayRepository.delete(it) }
        voteRecordRepository.findByMemberId(memberId)
            .forEach { voteRecordRepository.delete(it) }
        activityRepository.findByMemberId(memberId)
            .forEach { activityRepository.delete(it) }
        voteSummaryRepository.findById(questionId).ifPresent { voteSummaryRepository.delete(it) }
        dailyVoteUsageRepository
            .findByMemberIdAndUsageDate(memberId, LocalDate.now(ZoneOffset.UTC))
            ?.let { dailyVoteUsageRepository.delete(it) }
    }
}
