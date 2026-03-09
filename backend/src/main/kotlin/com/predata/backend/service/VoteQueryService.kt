package com.predata.backend.service

import com.predata.backend.config.properties.VoteProperties
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.policy.VotePolicy
import com.predata.backend.dto.VoteStatusResponse
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.DailyVoteUsageRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 투표 쿼리 서비스
 *
 * 읽기 전용. 투표 가능 여부, 중복 투표 여부, 잔여 일일 투표 수를 계산한다.
 * 쓰기 작업은 VoteCommandService 에서만 수행한다.
 */
@Service
@Transactional(readOnly = true)
class VoteQueryService(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val dailyVoteUsageRepository: DailyVoteUsageRepository,
    private val voteProperties: VoteProperties,
    private val memberRepository: MemberRepository,
) {
    /**
     * 회원이 특정 질문에 투표할 수 있는지 종합 판단.
     *
     * canVote=false 이면 reason 에 아래 값 중 하나가 담긴다:
     *   QUESTION_NOT_FOUND, VOTING_CLOSED, ALREADY_VOTED, DAILY_LIMIT_EXCEEDED
     */
    fun canVote(memberId: Long, questionId: Long): VoteStatusResponse {
        val now = LocalDateTime.now(ZoneOffset.UTC)

        val question = questionRepository.findById(questionId).orElse(null)
            ?: return VoteStatusResponse(canVote = false, reason = "QUESTION_NOT_FOUND")

        val isVotingOpen = question.status == QuestionStatus.VOTING && question.votingEndAt.isAfter(now)
        val alreadyVoted = alreadyVoted(memberId, questionId)
        val today = LocalDate.now(ZoneOffset.UTC)
        val usedCount = dailyVoteUsageRepository.findByMemberIdAndUsageDate(memberId, today)?.usedCount ?: 0
        val role = memberRepository.findById(memberId).orElse(null)?.role
        val availability = VotePolicy.evaluate(
            isVotingOpen = isVotingOpen,
            alreadyVoted = alreadyVoted,
            usedCount = usedCount,
            role = role,
            defaultDailyLimit = voteProperties.dailyLimit,
        )
        return VoteStatusResponse(
            canVote = availability.canVote,
            alreadyVoted = availability.alreadyVoted,
            remainingDailyVotes = availability.remainingVotes,
            reason = availability.reason?.name,
            voteVisibility = question.voteVisibility,
        )
    }

    /**
     * 회원이 해당 질문에 이미 투표했는지 확인한다.
     */
    fun alreadyVoted(memberId: Long, questionId: Long): Boolean =
        activityRepository.existsByMemberIdAndQuestionIdAndActivityType(
            memberId, questionId, ActivityType.VOTE,
        )

    /**
     * 오늘(UTC) 기준 남은 투표 횟수를 반환한다.
     * 사용 기록이 없으면 dailyLimit 전체를 반환한다.
     */
    fun remainingDailyVotes(memberId: Long): Int {
        val today = LocalDate.now(ZoneOffset.UTC)
        val used = dailyVoteUsageRepository.findByMemberIdAndUsageDate(memberId, today)?.usedCount ?: 0
        val role = memberRepository.findById(memberId).orElse(null)?.role
        return VotePolicy.remainingDailyVotes(used, role, voteProperties.dailyLimit)
    }
}
