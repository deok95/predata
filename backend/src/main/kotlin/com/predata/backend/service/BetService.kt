package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ActivityType
import com.predata.backend.dto.ActivityResponse
import com.predata.backend.dto.BetRequest
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.domain.QuestionPhase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BetService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val bettingBatchService: BettingBatchService,
    private val betRecordService: BetRecordService,
    private val bettingSuspensionService: BettingSuspensionService
) {

    /**
     * 베팅 실행
     * - 포인트 잔액 확인 및 차감
     * - 중복 베팅 방지
     * - 판돈 업데이트 (비관적 락)
     */
    @Transactional
    fun bet(request: BetRequest, clientIp: String? = null): ActivityResponse {
        // 1. 멤버 조회 및 밴/포인트 확인
        val member = memberRepository.findById(request.memberId)
            .orElse(null) ?: return ActivityResponse(
                success = false,
                message = "회원을 찾을 수 없습니다."
            )

        if (member.isBanned) {
            return ActivityResponse(
                success = false,
                message = "계정이 정지되었습니다. 사유: ${member.banReason ?: "이용약관 위반"}"
            )
        }

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

        if (question.status != QuestionStatus.BETTING) {
            return ActivityResponse(
                success = false,
                message = "베팅이 종료되었습니다."
            )
        }

        // phase 체크: Match 연결 질문은 phase == BETTING 일 때만 허용
        if (question.phase != null && question.phase != QuestionPhase.BETTING) {
            return ActivityResponse(
                success = false,
                message = "현재 베팅 가능한 phase가 아닙니다. (현재: ${question.phase})"
            )
        }

        // Match 연결 질문의 골 이벤트 베팅 정지 체크
        val matchId = question.match?.id
        if (matchId != null && bettingSuspensionService.isMatchBettingSuspended(matchId)) {
            return ActivityResponse(
                success = false,
                message = "골 이벤트로 베팅이 일시 정지되었습니다. 잠시 후 다시 시도해주세요."
            )
        }

        if (question.expiredAt.isBefore(LocalDateTime.now())) {
            return ActivityResponse(
                success = false,
                message = "베팅 기간이 만료되었습니다."
            )
        }

        // 3. 포인트 차감
        member.pointBalance -= request.amount
        memberRepository.save(member)

        // 4. 판돈 업데이트
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

        // 5. 베팅 기록 저장 (IP 포함)
        val activity = Activity(
            memberId = request.memberId,
            questionId = request.questionId,
            activityType = ActivityType.BET,
            choice = request.choice,
            amount = request.amount,
            latencyMs = request.latencyMs,
            ipAddress = clientIp
        )

        val savedActivity = activityRepository.save(activity)

        // 6. 온체인(Mock) 기록
        betRecordService.recordBet(savedActivity)

        // 7. 온체인 큐에 추가 (비동기 - 지갑 있는 경우)
        if (member.walletAddress != null) {
            bettingBatchService.enqueueBet(
                PendingBet(
                    questionId = request.questionId,
                    userAddress = member.walletAddress!!,
                    choice = request.choice.name,
                    amount = request.amount
                )
            )
        }

        return ActivityResponse(
            success = true,
            message = "베팅이 완료되었습니다.",
            activityId = savedActivity.id
        )
    }
}
