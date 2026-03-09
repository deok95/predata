package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.PoolStatus
import com.predata.backend.domain.Question
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.policy.SettlementPolicy
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.amm.UserSharesRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class SettlementPayoutSummary(
    val totalBets: Int,
    val totalWinners: Int,
    val totalPayout: BigDecimal,
)

@Service
class SettlementPayoutService(
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val transactionHistoryService: TransactionHistoryService,
    private val userSharesRepository: UserSharesRepository,
    private val marketPoolRepository: MarketPoolRepository,
    private val questionRepository: QuestionRepository,
    private val walletBalanceService: WalletBalanceService,
) {
    fun settleWinnerShares(questionId: Long, winningSide: ShareOutcome): SettlementPayoutSummary {
        val allShares = userSharesRepository.findByQuestionId(questionId)
        val memberIds = allShares.map { it.memberId }.distinct()
        val membersMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }

        var totalWinners = 0
        var totalPayout = BigDecimal.ZERO

        allShares.forEach { userShare ->
            if (!membersMap.containsKey(userShare.memberId)) return@forEach

            if (userShare.outcome == winningSide) {
                val payout = SettlementPolicy.payoutForWinningShares(userShare.shares)
                val wallet = walletBalanceService.credit(
                    memberId = userShare.memberId,
                    amount = payout,
                    txType = "AMM_SETTLEMENT",
                    referenceType = "QUESTION",
                    referenceId = questionId,
                    description = "AMM 정산 승리",
                )
                transactionHistoryService.record(
                    memberId = userShare.memberId,
                    type = "AMM_SETTLEMENT",
                    amount = payout,
                    balanceAfter = wallet.availableBalance,
                    description = "AMM 정산 승리 - Question #$questionId",
                    questionId = questionId
                )
                activityRepository.save(
                    Activity(
                        memberId = userShare.memberId,
                        questionId = questionId,
                        activityType = ActivityType.BET,
                        choice = if (userShare.outcome == ShareOutcome.YES) Choice.YES else Choice.NO,
                        amount = payout.toLong()
                    )
                )
                totalWinners++
                totalPayout = totalPayout.add(payout)
            }

            userShare.shares = BigDecimal.ZERO
            userShare.costBasisUsdc = BigDecimal.ZERO
            userSharesRepository.save(userShare)
        }

        return SettlementPayoutSummary(
            totalBets = allShares.size,
            totalWinners = totalWinners,
            totalPayout = totalPayout,
        )
    }

    fun settlePoolAfterPayout(questionId: Long, totalPayout: BigDecimal) {
        val pool = marketPoolRepository.findById(questionId).orElse(null) ?: return
        pool.collateralLocked = SettlementPolicy.nextCollateralLocked(pool.collateralLocked, totalPayout)
        pool.status = PoolStatus.SETTLED
        marketPoolRepository.save(pool)
    }

    fun finalizeQuestionAsSettled(question: Question) {
        question.status = com.predata.backend.domain.QuestionStatus.SETTLED
        question.disputeDeadline = null
        questionRepository.save(question)
    }
}
