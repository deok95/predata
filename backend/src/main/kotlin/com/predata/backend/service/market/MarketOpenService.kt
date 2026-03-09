package com.predata.backend.service.market

import com.predata.backend.config.properties.MarketBatchProperties
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.policy.MarketOpenDecision
import com.predata.backend.domain.policy.MarketOpenPolicy
import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.domain.market.QuestionMarketCandidate
import com.predata.backend.domain.market.SelectionStatus
import com.predata.backend.dto.amm.SeedPoolRequest
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import com.predata.backend.service.amm.SwapService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 마켓 오픈 서비스
 *
 * 질문 단위 트랜잭션(REQUIRES_NEW)으로 실패를 격리.
 * 배치 전체 트랜잭션 금지 → 부분 성공 허용.
 *
 * 실행 순서 (필수):
 *  1. seedPool 호출
 *  2. 성공 → question.status = BETTING (BREAK -> BETTING)
 *  3. 후보 row OPENED 기록
 *  실패 → OPEN_FAILED + 에러 기록, 질문 상태 미변경
 */
@Service
class MarketOpenService(
    private val questionRepository: QuestionRepository,
    private val candidateRepository: QuestionMarketCandidateRepository,
    private val swapService: SwapService,
    private val properties: MarketBatchProperties,
) {
    private val logger = LoggerFactory.getLogger(MarketOpenService::class.java)

    /**
     * SELECTED_TOP3 후보 전체 오픈 처리.
     * 실패 격리: 개별 실패가 전체를 멈추지 않는다.
     * @return (openedCount, failedCount)
     */
    fun openAll(candidates: List<QuestionMarketCandidate>): Pair<Int, Int> {
        val top3 = candidates.filter { it.selectionStatus == SelectionStatus.SELECTED_TOP3 }
        var openedCount = 0
        var failedCount = 0

        for (candidate in top3) {
            val success = openSingle(candidate)
            if (success) openedCount++ else failedCount++
        }

        return openedCount to failedCount
    }

    /**
     * 단일 후보 오픈. 별도 트랜잭션으로 실패를 격리.
     * @return true=성공, false=실패
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun openSingle(candidate: QuestionMarketCandidate): Boolean {
        val question = questionRepository.findById(candidate.questionId).orElse(null)
            ?: return recordFailure(candidate, "질문을 찾을 수 없습니다. questionId=${candidate.questionId}")

        when (MarketOpenPolicy.decideOpen(question.status)) {
            MarketOpenDecision.SUCCESS_ALREADY_BETTING -> {
                logger.info("[MarketOpen] questionId=${candidate.questionId} 이미 BETTING 상태 → 성공 처리")
                return recordSuccess(candidate)
            }
            MarketOpenDecision.FAILURE_INVALID_STATUS -> {
                return recordFailure(
                    candidate,
                    "오픈 불가 상태: status=${question.status}, questionId=${candidate.questionId}"
                )
            }
            MarketOpenDecision.OPEN_AND_TRANSITION -> Unit
        }

        return try {
            swapService.seedPool(
                SeedPoolRequest(
                    questionId = candidate.questionId,
                    seedUsdc = properties.seedUsdc,
                    feeRate = properties.feeRate,
                )
            )

            question.status = QuestionStatus.BETTING
            questionRepository.save(question)

            logger.info("[MarketOpen] questionId=${candidate.questionId} BETTING 전환 성공")
            recordSuccess(candidate)
        } catch (e: Exception) {
            // Pool already exists → 이전 시도에서 seedPool은 성공했으나 status 업데이트가 실패한 경우
            if (MarketOpenPolicy.isPoolAlreadyExistsRecoverable(e.message, question.status)) {
                logger.warn("[MarketOpen] questionId=${candidate.questionId} 기존 pool 감지 → BETTING 전환 완료")
                question.status = QuestionStatus.BETTING
                questionRepository.save(question)
                return recordSuccess(candidate)
            }
            logger.error("[MarketOpen] questionId=${candidate.questionId} 오픈 실패: ${e.message}", e)
            recordFailure(candidate, e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * OPEN_FAILED 후보만 재시도.
     */
    fun retryFailed(batchId: Long): Pair<Int, Int> {
        val failedCandidates = candidateRepository.findByBatchIdAndOpenStatus(batchId, OpenStatus.OPEN_FAILED)
        var openedCount = 0
        var failedCount = 0

        for (candidate in failedCandidates) {
            // 재시도 전 open_status 초기화
            candidate.openStatus = null
            candidate.openError = null
            candidate.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
            candidateRepository.save(candidate)

            val success = openSingle(candidate)
            if (success) openedCount++ else failedCount++
        }

        return openedCount to failedCount
    }

    private fun recordSuccess(candidate: QuestionMarketCandidate): Boolean {
        candidate.openStatus = OpenStatus.OPENED
        candidate.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        candidateRepository.save(candidate)
        return true
    }

    private fun recordFailure(candidate: QuestionMarketCandidate, error: String): Boolean {
        candidate.openStatus = MarketOpenPolicy.nextOpenStatus(success = false)
        candidate.openError = MarketOpenPolicy.truncateOpenError(error)
        candidate.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        candidateRepository.save(candidate)
        return false
    }
}
