package com.predata.backend.service

import com.predata.backend.config.properties.VoteProperties
import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.OnChainVoteRelay
import com.predata.backend.domain.policy.VotePolicy
import com.predata.backend.exception.AlreadyVotedException
import com.predata.backend.exception.DailyLimitExceededException
import com.predata.backend.exception.VotingClosedException
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.DailyVoteUsageRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.OnChainVoteRelayRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 투표 커맨드 서비스 — OBJECTIVE_RULE·VOTE_RESULT 공통 단일 경로
 *
 * 이 서비스는 질문 타입(settlementMode)에 관계없이 동일 규칙을 적용한다.
 *
 * [정책] hasVotingPass / 티켓 차감은 이 경로에서 강제하지 않는다.
 *   - /api/votes 는 표준 오픈 투표 경로로, 일일 투표 횟수(daily_vote_usage) 한도만 적용.
 *   - hasVotingPass·티켓 정책은 commit/reveal 경로(VoteCommitService)에서만 강제한다.
 *   - 이는 두 경로의 의도적 설계 차이이며, 정책 우회가 아니다:
 *     commit-reveal 기능이 비활성화된 환경(prod)에서는 /api/votes가 유일한 투표 경로.
 *
 * [집계] vote_summary는 /api/votes와 commit/reveal 양쪽이 업데이트하는 단일 진실 소스.
 *   결과 조회(VoteController.getResults)는 VoteSummary만 읽는다.
 *
 * 실행 순서:
 * 1. 질문 검증 (status=VOTING + votingEndAt > now(UTC))
 * 2. 중복 투표 검사
 * 3. daily_vote_usage UPSERT
 * 4. used_count 재조회 (VOTE-012: ROW_COUNT() 단독 의존 금지)
 * 5. Activity INSERT
 * 6. vote_summary UPSERT 증가
 * 7. onchain_vote_relays 릴레이 큐 삽입
 */
@Service
class VoteCommandService(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val dailyVoteUsageRepository: DailyVoteUsageRepository,
    private val voteSummaryRepository: VoteSummaryRepository,
    private val onChainVoteRelayRepository: OnChainVoteRelayRepository,
    private val voteRecordService: VoteRecordService,
    private val voteProperties: VoteProperties,
    private val memberRepository: MemberRepository,
    private val marketWebSocketService: MarketWebSocketService,
) {
    @Transactional
    fun vote(memberId: Long, questionId: Long, choice: Choice, clientIp: String? = null): Activity {
        val now = LocalDateTime.now(ZoneOffset.UTC)

        // 1. 질문 검증: status=VOTING && votingEndAt > now(UTC)
        questionRepository.findVotableById(questionId, now)
            ?: throw VotingClosedException()

        // 2. 중복 투표 검사
        if (activityRepository.existsByMemberIdAndQuestionIdAndActivityType(
                memberId, questionId, ActivityType.VOTE)) {
            throw AlreadyVotedException()
        }

        // 3. daily_vote_usage UPSERT
        val today = LocalDate.now(ZoneOffset.UTC)
        dailyVoteUsageRepository.upsertIncrement(memberId, today)

        // 4. used_count 재조회 → 한도 초과 시 예외 (트랜잭션 롤백으로 UPSERT 원상복구)
        val usedCount = dailyVoteUsageRepository.findByMemberIdAndUsageDate(memberId, today)?.usedCount ?: 1
        val role = memberRepository.findById(memberId).orElse(null)?.role
        val dailyLimit = VotePolicy.resolveDailyLimit(role, voteProperties.dailyLimit)
        if (usedCount > dailyLimit) {
            throw DailyLimitExceededException()
        }

        // 5. Activity INSERT + 6. vote_summary + 7. VoteRecord/relay
        // 경합 시 vote_records(uk_vr_member_question) DB unique 위반을 AlreadyVotedException으로 변환
        val activity = Activity(
            memberId = memberId,
            questionId = questionId,
            activityType = ActivityType.VOTE,
            choice = choice,
            amount = 0,
            ipAddress = clientIp,
        )
        val savedActivity = activityRepository.save(activity)

        // 6. vote_summary UPSERT 증가
        when (choice) {
            Choice.YES -> voteSummaryRepository.upsertIncrementYes(questionId)
            Choice.NO  -> voteSummaryRepository.upsertIncrementNo(questionId)
        }

        // Broadcast vote counts via WebSocket
        val summary = voteSummaryRepository.findById(questionId).orElse(null)
        if (summary != null) {
            marketWebSocketService.broadcastVoteUpdate(questionId, summary.yesCount, summary.noCount)
        }

        // 7. onchain 릴레이 큐 삽입 (VoteRecord → relay voteId 참조)
        // vote_records INSERT 경합 시 DataIntegrityViolationException → AlreadyVotedException 변환
        val voteRecord = try {
            voteRecordService.recordVote(savedActivity)
        } catch (e: DataIntegrityViolationException) {
            throw AlreadyVotedException()
        }
        onChainVoteRelayRepository.save(
            OnChainVoteRelay(
                voteId    = voteRecord.id!!,
                memberId  = memberId,
                questionId = questionId,
                choice    = choice,
            )
        )

        return savedActivity
    }
}
