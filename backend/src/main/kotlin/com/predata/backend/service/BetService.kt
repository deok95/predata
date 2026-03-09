package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ActivityType
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.domain.QuestionPhase
import com.predata.backend.dto.ActivityResponse
import com.predata.backend.dto.BetRequest
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class BetService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
    private val bettingBatchService: BettingBatchService,
    private val betRecordService: BetRecordService,
    private val transactionHistoryService: TransactionHistoryService,
    private val walletBalanceService: WalletBalanceService,
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
    fun bet(memberId: Long, request: BetRequest, clientIp: String? = null): ActivityResponse {

        // 1. 멤버 조회 및 밴/잔액 확인
        val member = memberRepository.findById(memberId)
            .orElse(null) ?: return ActivityResponse(
                success = false,
                message = "Member not found."
            )

        if (member.isBanned) {
            return ActivityResponse(
                success = false,
                message = "Account has been suspended. Reason: ${member.banReason ?: "Terms of Service violation"}"
            )
        }

        // 베팅 금액 한도 검증 (1~100 USDC)
        val betAmount = BigDecimal(request.amount)
        if (betAmount < BET_MIN_USDC || betAmount > BET_MAX_USDC) {
            return ActivityResponse(
                success = false,
                message = "Bet amount must be between ${BET_MIN_USDC} and ${BET_MAX_USDC} USDC."
            )
        }

        val availableBalance = walletBalanceService.getAvailableBalance(memberId)
        if (availableBalance < betAmount) {
            return ActivityResponse(
                success = false,
                message = "Insufficient USDC balance. (Current: $availableBalance, Required: $betAmount)"
            )
        }

        // 2. 질문 조회 (비관적 락)
        val question = questionRepository.findByIdWithLock(request.questionId)
            ?: return ActivityResponse(
                success = false,
                message = "Question not found."
            )

        if (question.status != QuestionStatus.BETTING) {
            return ActivityResponse(
                success = false,
                message = "Betting has ended."
            )
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val linkedMatch = question.match
        val isLiveMatch = linkedMatch != null && (
            linkedMatch.matchStatus == MatchStatus.LIVE || linkedMatch.matchStatus == MatchStatus.HALFTIME
        )
        val isClosedMatch = linkedMatch != null && (
            linkedMatch.matchStatus == MatchStatus.FINISHED ||
                linkedMatch.matchStatus == MatchStatus.CANCELLED ||
                linkedMatch.matchStatus == MatchStatus.POSTPONED ||
                question.phase == QuestionPhase.FINISHED ||
                question.phase == QuestionPhase.SETTLED
        )

        if (isClosedMatch) {
            return ActivityResponse(
                success = false,
                message = "This match has ended."
            )
        }

        // 라이브/연장(ET) 경기에서는 expiredAt보다 실제 경기 종료 이벤트를 우선한다.
        if (!isLiveMatch && question.expiredAt.isBefore(now)) {
            return ActivityResponse(
                success = false,
                message = "Betting period has expired."
            )
        }

        // 3. USDC 잔액 차감 (wallet ledger + member 잔액 동기화)
        val wallet = walletBalanceService.debit(
            memberId = memberId,
            amount = betAmount,
            txType = "BET",
            referenceType = "QUESTION",
            referenceId = request.questionId,
            description = "레거시 베팅 차감",
        )

        transactionHistoryService.record(
            memberId = memberId,
            type = "BET",
            amount = betAmount.negate(),
            balanceAfter = wallet.availableBalance,
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
            message = "Bet placed successfully.",
            activityId = savedActivity.id
        )
    }
}
