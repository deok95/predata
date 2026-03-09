package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.dto.ActivityResponse
import com.predata.backend.dto.VoteRequest
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * @deprecated 레거시 투표 서비스. hasVotingPass 의존 로직 포함.
 * 신규 투표 플로우는 POST /api/votes (VoteController) 를 사용하세요.
 * 기존 코드 삭제 금지 — 비활성 경로 유지.
 */
@Deprecated("레거시 투표 서비스. 신규 투표 플로우에서 이 서비스를 호출하지 마세요.")
@Service
class VoteService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: com.predata.backend.repository.MemberRepository,
    private val voteRecordService: VoteRecordService
) {

    /**
     * @deprecated 레거시 투표 실행. hasVotingPass 확인 로직 포함.
     * 신규 투표 플로우는 VoteController.vote() 를 사용하세요.
     */
    @Deprecated("레거시 투표 실행. VoteController.vote() 를 사용하세요.")
    @Transactional
    fun vote(memberId: Long, request: VoteRequest, clientIp: String? = null): ActivityResponse {

        // 1. 밴 체크
        val member = memberRepository.findById(memberId).orElse(null)
        if (member != null && member.isBanned) {
            return ActivityResponse(
                success = false,
                message = "Account has been suspended. Reason: ${member.banReason ?: "Terms of Service violation"}"
            )
        }

        // [LEGACY] 투표 패스 확인 — hasVotingPass 의존 레거시 로직
        if (member == null || !member.hasVotingPass) {
            return ActivityResponse(
                success = false,
                message = "Voting pass required. Please purchase one from your My Page."
            )
        }

        // 2. 질문 존재 여부 확인
        val question = questionRepository.findById(request.questionId)
            .orElse(null) ?: return ActivityResponse(
                success = false,
                message = "Question not found."
            )

        if (question.status != QuestionStatus.VOTING) {
            return ActivityResponse(
                success = false,
                message = "Voting has ended."
            )
        }

        // 투표 종료 시간 체크
        if (question.votingEndAt.isBefore(LocalDateTime.now())) {
            return ActivityResponse(
                success = false,
                message = "Voting period has ended."
            )
        }

        // 3. 중복 투표 방지
        val alreadyVoted = activityRepository.existsByMemberIdAndQuestionIdAndActivityType(
            memberId,
            request.questionId,
            ActivityType.VOTE
        )

        if (alreadyVoted) {
            return ActivityResponse(
                success = false,
                message = "You have already voted."
            )
        }

        // 4. 투표 기록 저장 (IP 포함)
        val activity = Activity(
            memberId = memberId,
            questionId = request.questionId,
            activityType = ActivityType.VOTE,
            choice = request.choice,
            amount = 0,
            latencyMs = request.latencyMs,
            ipAddress = clientIp
        )

        val savedActivity = activityRepository.save(activity)

        // 5. 온체인(Mock) 기록
        voteRecordService.recordVote(savedActivity)

        return ActivityResponse(
            success = true,
            message = "Vote completed successfully.",
            activityId = savedActivity.id
        )
    }
}
