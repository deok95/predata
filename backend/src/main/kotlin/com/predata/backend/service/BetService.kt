package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.dto.ActivityResponse
import com.predata.backend.dto.BetRequest
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BetService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository
) {

    /**
     * 베팅 실행
     * - 포인트 잔액 확인 및 차감
     * - 중복 베팅 방지
     * - 판돈 업데이트 (비관적 락)
     */
    @Transactional
    fun bet(request: BetRequest): ActivityResponse {
        // 1. 멤버 조회 및 포인트 확인
        val member = memberRepository.findById(request.memberId)
            .orElse(null) ?: return ActivityResponse(
                success = false,
                message = "회원을 찾을 수 없습니다."
            )

        if (member.pointBalance < request.amount) {
            return ActivityResponse(
                success = false,
                message = "포인트가 부족합니다. (보유: ${member.pointBalance}, 필요: ${request.amount})"
            )
        }

        // 2. 질문 조회 (비관적 락)
        val question = questionRepository.findByIdWithLock(request.questionId)
            ?: return ActivityResponse(
                success = false,
                message = "질문을 찾을 수 없습니다."
            )

        if (question.status != "OPEN") {
            return ActivityResponse(
                success = false,
                message = "베팅이 종료되었습니다."
            )
        }

        // 3. 중복 베팅 방지
        val alreadyBet = activityRepository.existsByMemberIdAndQuestionIdAndActivityType(
            request.memberId,
            request.questionId,
            ActivityType.BET
        )

        if (alreadyBet) {
            return ActivityResponse(
                success = false,
                message = "이미 베팅하셨습니다."
            )
        }

        // 4. 포인트 차감
        member.pointBalance -= request.amount
        memberRepository.save(member)

        // 5. 판돈 업데이트
        when (request.choice) {
            com.predata.backend.domain.Choice.YES -> {
                question.yesBetPool += request.amount
            }
            com.predata.backend.domain.Choice.NO -> {
                question.noBetPool += request.amount
            }
        }
        question.totalBetPool += request.amount
        questionRepository.save(question)

        // 6. 베팅 기록 저장
        val activity = Activity(
            memberId = request.memberId,
            questionId = request.questionId,
            activityType = ActivityType.BET,
            choice = request.choice,
            amount = request.amount,
            latencyMs = request.latencyMs
        )

        val savedActivity = activityRepository.save(activity)

        return ActivityResponse(
            success = true,
            message = "베팅이 완료되었습니다.",
            activityId = savedActivity.id
        )
    }
}
