package com.predata.backend.service

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.policy.SettlementPolicy
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.settlement.SettlementCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class SettlementService(
    private val questionRepository: QuestionRepository,
    private val blockchainService: BlockchainService,
    private val settlementCalculator: SettlementCalculator,
    private val auditService: AuditService,
    private val settlementPostProcessService: SettlementPostProcessService,
    private val settlementPayoutService: SettlementPayoutService,
    private val settlementHistoryQueryService: SettlementHistoryQueryService,
    private val userSharesRepository: com.predata.backend.repository.amm.UserSharesRepository,
    private val marketPoolRepository: com.predata.backend.repository.amm.MarketPoolRepository,
) {
    /**
     * 1단계: 정산 시작 (PENDING_SETTLEMENT)
     * 결과와 근거를 기록하고 이의 제기 기간을 시작한다.
     * 배당 분배는 아직 하지 않는다.
     */
    @Transactional
    fun initiateSettlement(questionId: Long, finalResult: FinalResult, sourceUrl: String?): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        SettlementPolicy.ensureCanInitiate(question.status)

        question.status = QuestionStatus.SETTLED
        question.finalResult = finalResult
        question.sourceUrl = sourceUrl
        val delayHours = resolveSettlementDelayHours(question, sourceUrl)

        // 외부데이터 확보된 케이스는 즉시 확정 가능, 미확보는 12시간 지연
        question.disputeDeadline = if (delayHours == 0L) {
            LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)
        } else {
            LocalDateTime.now(ZoneOffset.UTC).plusHours(delayHours)
        }
        questionRepository.save(question)

        // Audit log: 정산 시작
        auditService.log(
            memberId = null,
            action = com.predata.backend.domain.AuditAction.SETTLE,
            entityType = "QUESTION",
            entityId = questionId,
            detail = "Settlement initiated: ${finalResult.name}"
        )

        // AMM_FPMM 정산 대상 확인
        marketPoolRepository.findById(questionId).orElseThrow {
            IllegalStateException("AMM 마켓 풀이 없습니다.")
        }
        val totalBets = userSharesRepository.findByQuestionId(questionId).size

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = totalBets,
            totalWinners = 0,
            totalPayout = 0,
            voterRewards = 0,
            message = if (delayHours == 0L)
                "Settlement initiated. External source verified; eligible for immediate finalization."
            else
                "Settlement initiated. Finalization available after ${delayHours} hour(s)."
        )
    }

    /**
     * 2단계: 정산 확정 (SETTLED)
     * 이의 제기 기간이 지난 후 배당금 분배를 실행한다.
     * 관리자가 강제 확정할 경우 skipDeadlineCheck = true
     *
     * 최적화: 3개 서비스(Settlement/Tier/Reward) 로직을 통합
     * - 1회 조회 → 메모리 처리 → 1회 저장
     */
    @Transactional
    fun finalizeSettlement(questionId: Long, skipDeadlineCheck: Boolean = false): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        SettlementPolicy.ensureCanFinalize(
            status = question.status,
            disputeDeadline = question.disputeDeadline,
            nowUtc = LocalDateTime.now(ZoneOffset.UTC),
            skipDeadlineCheck = skipDeadlineCheck,
        )

        return finalizeAmmSettlement(question)
    }

    /**
     * AMM (FPMM) 정산 로직
     * user_shares 기반으로 정산하고, 기존 오더북 로직과 완전히 분리됨
     */
    @Transactional
    private fun finalizeAmmSettlement(question: Question): SettlementResult {
        val questionId = question.id ?: throw IllegalStateException("Question ID is null")
        val finalResult = question.finalResult

        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("Cannot finalize settlement without confirmed final result.")
        }

        val winningSide: ShareOutcome = SettlementPolicy.resolveWinningSide(finalResult)
            ?: throw IllegalStateException("Unsupported result: $finalResult")

        val payoutSummary = settlementPayoutService.settleWinnerShares(questionId, winningSide)
        settlementPayoutService.settlePoolAfterPayout(questionId, payoutSummary.totalPayout)
        settlementPayoutService.finalizeQuestionAsSettled(question)
        blockchainService.settleQuestionOnChain(questionId, finalResult)

        settlementPostProcessService.distributeCreatorShareNonBlocking(questionId, question.creatorMemberId)
        val voterRewardsAmount = settlementPostProcessService.distributeVoterRewardsNonBlocking(questionId)
        settlementPostProcessService.transitionToRewardDistributedNonBlocking(questionId, voterRewardsAmount)

        return SettlementResult(
            questionId = questionId,
            finalResult = finalResult.name,
            totalBets = payoutSummary.totalBets,
            totalWinners = payoutSummary.totalWinners,
            totalPayout = payoutSummary.totalPayout.setScale(0, RoundingMode.DOWN).toLong(),
            voterRewards = voterRewardsAmount,
            message = "AMM settlement finalized."
        )
    }

    private fun resolveSettlementDelayHours(question: Question, sourceUrl: String?): Long {
        val hasExternalEvidence = !sourceUrl.isNullOrBlank()
        return SettlementPolicy.resolveDelayHours(question.marketType, hasExternalEvidence)
    }

    /**
     * 사용자별 정산 내역 조회 (Activity 기반)
     */
    fun getSettlementHistory(memberId: Long): List<SettlementHistoryItem> {
        return settlementHistoryQueryService.getSettlementHistory(memberId)
    }
}

/**
 * 정산 결과 DTO
 */
data class SettlementResult(
    val questionId: Long,
    val finalResult: String,
    val totalBets: Int,
    val totalWinners: Int,
    val totalPayout: Long,
    val voterRewards: Long,
    val message: String
)

/**
 * 정산 내역 DTO
 */
data class SettlementHistoryItem(
    val questionId: Long,
    val questionTitle: String,
    val myChoice: String,
    val finalResult: String,
    val betAmount: Long,
    val payout: Long,
    val profit: Long,
    val isWinner: Boolean
)
