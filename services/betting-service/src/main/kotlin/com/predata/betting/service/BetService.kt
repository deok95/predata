package com.predata.betting.service

import com.predata.common.client.MemberClient
import com.predata.common.client.QuestionClient
import com.predata.common.config.EventPublisher
import com.predata.common.domain.ActivityType
import com.predata.common.domain.Choice
import com.predata.common.dto.PointsRequest
import com.predata.common.dto.UpdatePoolRequest
import com.predata.common.events.BetPlacedEvent
import com.predata.betting.domain.Activity
import com.predata.betting.repository.ActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BetService(
    private val activityRepository: ActivityRepository,
    private val memberClient: MemberClient,
    private val questionClient: QuestionClient,
    private val eventPublisher: EventPublisher
) {
    private val log = LoggerFactory.getLogger(BetService::class.java)

    @Transactional
    fun placeBet(request: PlaceBetRequest): Activity {
        // 1. Member 조회
        val memberResponse = memberClient.getMember(request.memberId)
        val member = memberResponse.data ?: throw IllegalArgumentException("회원을 찾을 수 없습니다.")

        // 2. 포인트 확인
        if (member.pointBalance < request.amount) {
            throw IllegalArgumentException("포인트가 부족합니다.")
        }

        // 3. Question 조회
        val questionResponse = questionClient.getQuestion(request.questionId)
        if (!questionResponse.success || questionResponse.data == null) {
            throw IllegalArgumentException("질문을 찾을 수 없습니다.")
        }

        // 4. 중복 베팅 방지
        if (activityRepository.existsByMemberIdAndQuestionIdAndActivityType(
                request.memberId, request.questionId, ActivityType.BET)) {
            throw IllegalArgumentException("이미 베팅하셨습니다.")
        }

        // 5. 포인트 차감
        memberClient.deductPoints(request.memberId, PointsRequest(request.amount))

        // 6. 판돈 업데이트
        questionClient.updateBetPool(
            request.questionId,
            UpdatePoolRequest(request.choice.name, request.amount)
        )

        // 7. Activity 저장
        val activity = Activity(
            memberId = request.memberId,
            questionId = request.questionId,
            activityType = ActivityType.BET,
            choice = request.choice,
            amount = request.amount
        )
        val saved = activityRepository.save(activity)

        // 8. 이벤트 발행 (Redis 미연결 시 무시)
        try {
            eventPublisher.publishBetPlacedEvent(
                BetPlacedEvent(
                    questionId = request.questionId,
                    memberId = request.memberId,
                    userAddress = member.walletAddress ?: "",
                    choice = request.choice.name,
                    amount = request.amount
                )
            )
        } catch (e: Exception) {
            log.warn("Redis 이벤트 발행 실패 (무시): ${e.message}")
        }

        return saved
    }

    @Transactional(readOnly = true)
    fun getActivitiesByQuestion(questionId: Long, type: ActivityType?): List<Activity> {
        return if (type != null) {
            activityRepository.findByQuestionIdAndActivityType(questionId, type)
        } else {
            activityRepository.findByQuestionId(questionId)
        }
    }

    @Transactional(readOnly = true)
    fun getActivitiesByMember(memberId: Long): List<Activity> {
        return activityRepository.findByMemberId(memberId)
    }
}

data class PlaceBetRequest(
    val memberId: Long,
    val questionId: Long,
    val choice: Choice,
    val amount: Long
)
