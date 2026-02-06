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

@Service
class VoteService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: com.predata.backend.repository.MemberRepository,
    private val ticketService: TicketService
) {

    /**
     * 투표 실행
     * - 밴 유저 차단
     * - 5-Lock 티켓 차감
     * - 중복 투표 방지
     * - 무지성 투표 방지 (latency 체크)
     */
    @Transactional
    fun vote(request: VoteRequest, clientIp: String? = null): ActivityResponse {
        // 0. 밴 체크
        val member = memberRepository.findById(request.memberId).orElse(null)
        if (member != null && member.isBanned) {
            return ActivityResponse(
                success = false,
                message = "계정이 정지되었습니다. 사유: ${member.banReason ?: "이용약관 위반"}"
            )
        }

        // 1. 질문 존재 여부 확인
        val question = questionRepository.findById(request.questionId)
            .orElse(null) ?: return ActivityResponse(
                success = false,
                message = "질문을 찾을 수 없습니다."
            )

        if (question.status != QuestionStatus.VOTING) {
            return ActivityResponse(
                success = false,
                message = "투표가 종료되었습니다."
            )
        }

        // 투표 종료 시간 체크
        if (question.votingEndAt.isBefore(LocalDateTime.now())) {
            return ActivityResponse(
                success = false,
                message = "투표 기간이 종료되었습니다."
            )
        }

        // 2. 중복 투표 방지
        val alreadyVoted = activityRepository.existsByMemberIdAndQuestionIdAndActivityType(
            request.memberId,
            request.questionId,
            ActivityType.VOTE
        )

        if (alreadyVoted) {
            return ActivityResponse(
                success = false,
                message = "이미 투표하셨습니다."
            )
        }

        // 3. 티켓 차감 (5-Lock)
        val ticketConsumed = ticketService.consumeTicket(request.memberId)
        if (!ticketConsumed) {
            return ActivityResponse(
                success = false,
                message = "오늘의 투표 티켓을 모두 사용하셨습니다."
            )
        }

        // 4. 투표 기록 저장 (IP 포함)
        val activity = Activity(
            memberId = request.memberId,
            questionId = request.questionId,
            activityType = ActivityType.VOTE,
            choice = request.choice,
            amount = 0,
            latencyMs = request.latencyMs,
            ipAddress = clientIp
        )

        val savedActivity = activityRepository.save(activity)
        val remainingTickets = ticketService.getRemainingTickets(request.memberId)

        return ActivityResponse(
            success = true,
            message = "투표가 완료되었습니다.",
            activityId = savedActivity.id,
            remainingTickets = remainingTickets
        )
    }
}
