package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ActivityType
import com.predata.backend.dto.ActivityResponse
import com.predata.backend.dto.BetRequest
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BetService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val bettingBatchService: BettingBatchService,
    private val betRecordService: BetRecordService,
    private val transactionHistoryService: TransactionHistoryService
) {

    companion object {
        val BET_MIN_USDC = BigDecimal.ONE      // 최소 1 USDC
        val BET_MAX_USDC = BigDecimal(100)     // 최대 100 USDC
    }

    /**
     * 베팅 실행
     * - USDC 잔액 확인 및 차감
     * - 베팅 한도: 1~100 USDC
     * - 판돈 업데이트 (비관적 락)
     */
    @Transactional
    fun bet(request: BetRequest, clientIp: String? = null): ActivityResponse {
        // 0. memberId 검증
        val memberId = request.memberId ?: return ActivityResponse(
            success = false,
            message = "회원 ID가 필요합니다."
        )

        // 1. 멤버 조회 및 밴/잔액 확인
        val member = memberRepository.findById(memberId)
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

        // 베팅 금액 한도 검증 (1~100 USDC)
        val betAmount = BigDecimal(request.amount)
        if (betAmount < BET_MIN_USDC || betAmount > BET_MAX_USDC) {
            return ActivityResponse(
                success = false,
                message = "베팅 금액은 ${BET_MIN_USDC}~${BET_MAX_USDC} USDC 범위여야 합니다."
            )
        }

        if (member.usdcBalance < betAmount) {
            return ActivityResponse(
                success = false,
                message = "USDC 잔액이 부족합니다. (보유: ${member.usdcBalance}, 필요: $betAmount)"
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

        if (question.expiredAt.isBefore(LocalDateTime.now())) {
            return ActivityResponse(
                success = false,
                message = "베팅 기간이 만료되었습니다."
            )
        }

        // 3. USDC 잔액 차감
        member.usdcBalance = member.usdcBalance.subtract(betAmount)
        memberRepository.save(member)

        transactionHistoryService.record(
            memberId = memberId,
            type = "BET",
            amount = betAmount.negate(),
            balanceAfter = member.usdcBalance,
            description = "베팅 - Question #${request.questionId} ${request.choice}",
            questionId = request.questionId
        )

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
            memberId = memberId,
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
