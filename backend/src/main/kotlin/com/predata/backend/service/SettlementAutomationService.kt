package com.predata.backend.service

import com.predata.backend.domain.AuditAction
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.VotingPhase
import com.predata.backend.domain.policy.SettlementPolicy
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.settlement.adapters.ResolutionAdapterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SettlementAutomationService(
    private val questionRepository: QuestionRepository,
    private val resolutionAdapterRegistry: ResolutionAdapterRegistry,
    private val settlementService: SettlementService,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(SettlementAutomationService::class.java)

    companion object {
        private const val AUTO_SETTLE_MIN_CONFIDENCE = 0.99
    }

    @Transactional
    fun initiateSettlementAuto(questionId: Long): SettlementResult {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        SettlementPolicy.ensureCanInitiate(question.status)

        val resolutionResult = resolutionAdapterRegistry.resolve(question)
        val finalResult = resolutionResult.result
            ?: throw IllegalStateException("Settlement result not yet confirmed.")
        if (finalResult == FinalResult.PENDING) {
            throw IllegalStateException("Settlement result not yet confirmed.")
        }

        return settlementService.initiateSettlement(questionId, finalResult, resolutionResult.sourceUrl)
    }

    @Transactional
    fun autoSettleWithVerification(questionId: Long): SettlementResult? {
        val question = questionRepository.findByIdWithLock(questionId)
            ?: throw IllegalArgumentException("Question not found.")

        // VOTE_RESULT 질문: reveal 단계 완료(VOTING_REVEAL_CLOSED) 전 자동 정산 차단
        if (question.voteResultSettlement) {
            if (question.votingPhase != VotingPhase.VOTING_REVEAL_CLOSED) {
                logger.info("[AutoSettle] 질문 #{} VOTE_RESULT reveal 미완료 (phase={}), 대기", questionId, question.votingPhase)
                return null
            }
        } else if (question.marketType != MarketType.VERIFIABLE) {
            logger.info("[AutoSettle] 질문 #{} VERIFIABLE이 아님, 수동 정산 대기", questionId)
            return null
        }

        val resolutionResult = try {
            resolutionAdapterRegistry.resolve(question)
        } catch (e: Exception) {
            logger.error("[AutoSettle] Question #{} result fetch failed: {}", questionId, e.message)
            return null
        }

        val result = resolutionResult.result
        val confidence = resolutionResult.confidence

        if (result == null || confidence == null) {
            logger.info("[AutoSettle] 질문 #{} 결과 불완전 (result={}, confidence={}), 재시도 대기", questionId, result, confidence)
            return null
        }
        if (confidence < AUTO_SETTLE_MIN_CONFIDENCE) {
            logger.info("[AutoSettle] 질문 #{} confidence 부족 ({}), 수동 정산 대기", questionId, confidence)
            return null
        }

        // VERIFIABLE 전용: 소스 URL 존재 여부 확인
        // 완료 상태 판정(FINISHED 여부)은 각 어댑터가 confidence=1.0으로 보증하므로 문자열 검사 불필요
        val sourceUrl = if (!question.voteResultSettlement) {
            val url = resolutionResult.sourceUrl
            if (url == null) {
                logger.info("[AutoSettle] 질문 #{} 소스 URL 없음, 재시도 대기", questionId)
                return null
            }
            url
        } else {
            resolutionResult.sourceUrl
        }

        auditService.log(
            memberId = null,
            action = AuditAction.SETTLE,
            entityType = "QUESTION",
            entityId = questionId,
            detail = "Auto-settlement verified: ${result.name}, confidence=$confidence"
        )

        settlementService.initiateSettlement(questionId, result, sourceUrl)
        return settlementService.finalizeSettlement(questionId, skipDeadlineCheck = true)
    }
}
