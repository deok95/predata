package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.dto.SellBetRequest
import com.predata.backend.dto.SellBetResponse
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class BetSellService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val transactionHistoryService: TransactionHistoryService
) {

    companion object {
        const val INITIAL_LIQUIDITY = 500L
    }

    /**
     * 베팅 판매 실행
     * - 베팅 소유권 및 상태 확인
     * - 현재 풀 비율 기반 환불 계산 (AMM 공식)
     * - 풀 업데이트 및 포인트 환불
     * - BET_SELL 활동 기록 생성
     */
    @Transactional
    fun sellBet(request: SellBetRequest, clientIp: String? = null): SellBetResponse {
        // 1. 베팅 조회 및 검증
        val originalBet = activityRepository.findById(request.betId)
            .orElse(null) ?: return SellBetResponse(
                success = false,
                message = "베팅을 찾을 수 없습니다."
            )

        // 소유권 확인
        if (originalBet.memberId != request.memberId) {
            return SellBetResponse(
                success = false,
                message = "본인의 베팅만 판매할 수 있습니다."
            )
        }

        // 타입 확인
        if (originalBet.activityType != ActivityType.BET) {
            return SellBetResponse(
                success = false,
                message = "베팅 활동만 판매할 수 있습니다."
            )
        }

        // 중복 판매 확인
        val alreadySold = activityRepository.findByParentBetIdAndActivityType(
            request.betId,
            ActivityType.BET_SELL
        )
        if (alreadySold != null) {
            return SellBetResponse(
                success = false,
                message = "이미 판매된 베팅입니다."
            )
        }

        // 2. 질문 조회 (비관적 락)
        val question = questionRepository.findByIdWithLock(originalBet.questionId)
            ?: return SellBetResponse(
                success = false,
                message = "질문을 찾을 수 없습니다."
            )

        // 상태 확인: BETTING 상태에서만 판매 가능
        if (question.status != QuestionStatus.BETTING) {
            return SellBetResponse(
                success = false,
                message = "베팅 기간에만 판매할 수 있습니다."
            )
        }

        // 만료 확인
        if (question.expiredAt.isBefore(LocalDateTime.now())) {
            return SellBetResponse(
                success = false,
                message = "베팅 기간이 만료되었습니다."
            )
        }

        // 3. 회원 조회 및 밴 확인
        val member = memberRepository.findById(request.memberId)
            .orElse(null) ?: return SellBetResponse(
                success = false,
                message = "회원을 찾을 수 없습니다."
            )

        if (member.isBanned) {
            return SellBetResponse(
                success = false,
                message = "계정이 정지되었습니다. 사유: ${member.banReason ?: "이용약관 위반"}"
            )
        }

        // 4. 환불 금액 계산 (AMM 공식)
        val winningPool = when (originalBet.choice) {
            com.predata.backend.domain.Choice.YES -> question.yesBetPool
            com.predata.backend.domain.Choice.NO -> question.noBetPool
        }

        val refundAmount = calculateRefund(
            betAmount = originalBet.amount,
            totalPool = question.totalBetPool,
            winningPool = winningPool
        )

        // 환불 금액 검증
        if (refundAmount < 0) {
            return SellBetResponse(
                success = false,
                message = "환불 금액 계산 오류가 발생했습니다."
            )
        }

        // 5. 풀 업데이트
        when (originalBet.choice) {
            com.predata.backend.domain.Choice.YES -> {
                question.yesBetPool -= refundAmount
            }
            com.predata.backend.domain.Choice.NO -> {
                question.noBetPool -= refundAmount
            }
        }
        question.totalBetPool -= refundAmount

        // 풀 음수 방지 검증
        if (question.yesBetPool < 0 || question.noBetPool < 0 || question.totalBetPool < 0) {
            return SellBetResponse(
                success = false,
                message = "풀 업데이트 중 오류가 발생했습니다."
            )
        }

        questionRepository.save(question)

        // 6. USDC 환불
        member.usdcBalance = member.usdcBalance.add(BigDecimal(refundAmount))
        memberRepository.save(member)

        transactionHistoryService.record(
            memberId = request.memberId,
            type = "SETTLEMENT",
            amount = BigDecimal(refundAmount),
            balanceAfter = member.usdcBalance,
            description = "베팅 판매 환불 - Question #${originalBet.questionId}",
            questionId = originalBet.questionId
        )

        // 7. BET_SELL 활동 기록 생성
        val sellActivity = Activity(
            memberId = request.memberId,
            questionId = originalBet.questionId,
            activityType = ActivityType.BET_SELL,
            choice = originalBet.choice,
            amount = refundAmount,
            latencyMs = null,
            ipAddress = clientIp,
            parentBetId = request.betId
        )

        val savedActivity = activityRepository.save(sellActivity)

        // 8. 응답 반환
        return SellBetResponse(
            success = true,
            message = "베팅이 성공적으로 판매되었습니다.",
            originalBetAmount = originalBet.amount,
            refundAmount = refundAmount,
            profit = refundAmount - originalBet.amount,
            newPoolYes = question.yesBetPool,
            newPoolNo = question.noBetPool,
            newPoolTotal = question.totalBetPool,
            sellActivityId = savedActivity.id
        )
    }

    /**
     * 환불 금액 계산 (AMM 공식)
     * SettlementService.calculatePayout과 동일한 로직 사용
     */
    private fun calculateRefund(
        betAmount: Long,
        totalPool: Long,
        winningPool: Long
    ): Long {
        val effectiveTotalPool = maxOf(0L, totalPool - INITIAL_LIQUIDITY * 2)
        val effectiveWinningPool = maxOf(0L, winningPool - INITIAL_LIQUIDITY)

        if (effectiveWinningPool == 0L) return betAmount

        val payoutRatio = BigDecimal(effectiveTotalPool)
            .divide(BigDecimal(effectiveWinningPool), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal("0.99")) // 1% fee

        return BigDecimal(betAmount)
            .multiply(payoutRatio)
            .setScale(0, RoundingMode.DOWN)
            .toLong()
    }
}
