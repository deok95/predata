package com.predata.backend.service.market

import com.predata.backend.config.properties.MarketBatchProperties
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.policy.Top3CandidateInput
import com.predata.backend.domain.policy.Top3SelectionPolicy
import com.predata.backend.domain.market.MarketOpenBatch
import com.predata.backend.domain.market.QuestionMarketCandidate
import com.predata.backend.domain.market.SelectionReason
import com.predata.backend.domain.market.SelectionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.repository.market.MarketCategoryRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 카테고리별 Top3 선별 서비스
 *
 * 정렬 규칙 (고정):
 *  1. vote_count DESC
 *  2. created_at ASC
 *  3. question_id ASC
 *
 * 후보 < 3이면 있는 만큼 오픈. 0개 카테고리는 스킵.
 * market_categories 테이블의 is_active=true 카테고리만 처리.
 */
@Service
class CategoryTop3SelectionService(
    private val questionRepository: QuestionRepository,
    private val voteSummaryRepository: VoteSummaryRepository,
    private val candidateRepository: QuestionMarketCandidateRepository,
    private val categoryRepository: MarketCategoryRepository,
    private val properties: MarketBatchProperties,
) {
    private val logger = LoggerFactory.getLogger(CategoryTop3SelectionService::class.java)

    /**
     * BREAK 대기(투표 종료 + 5분 경과) 질문을 카테고리별 Top3로 선별 후 DB에 저장.
     * market_categories.is_active=true 카테고리에 속하는 질문만 처리.
     * @return 저장된 전체 후보 목록 (SELECTED_TOP3 + NOT_SELECTED)
     */
    @Transactional
    fun selectAndSave(batch: MarketOpenBatch, cutoff: LocalDateTime): List<QuestionMarketCandidate> {
        val recovered = promoteExpiredVotingToBreak(cutoff)
        if (recovered > 0) {
            logger.info("[MarketBatch#${batch.id}] 만료 VOTING -> BREAK 자동 복구: ${recovered}건")
        }

        val activeCategories = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
            .map { it.code }
            .toSet()

        if (activeCategories.isEmpty()) {
            logger.warn("[MarketBatch#${batch.id}] 활성 카테고리 없음. 선별 스킵.")
            return emptyList()
        }

        val expiredQuestions = questionRepository.findBreakReadyForMarketOpenBefore(cutoff)
            .filter { (it.category ?: "GENERAL") in activeCategories }

        if (expiredQuestions.isEmpty()) {
            logger.info("[MarketBatch#${batch.id}] 오픈 가능한 BREAK 질문 없음 (활성 카테고리 기준). cutoff=$cutoff")
            return emptyList()
        }

        val summaryMap = voteSummaryRepository
            .findAllById(expiredQuestions.map { it.id!! })
            .associateBy { it.questionId }

        val byCategory = expiredQuestions.groupBy { it.category ?: "GENERAL" }

        val allCandidates = mutableListOf<QuestionMarketCandidate>()
        val now = LocalDateTime.now(ZoneOffset.UTC)

        for ((category, questions) in byCategory) {
            val ranked = Top3SelectionPolicy.rankAndSelect(
                inputs = questions.map { question ->
                    Top3CandidateInput(
                        questionId = question.id!!,
                        voteCount = summaryMap[question.id]?.totalCount ?: 0L,
                        createdAt = question.createdAt,
                        normalizedHash = question.questionNormalizedHash,
                    )
                },
                minOpenPerCategory = properties.minOpenPerCategory,
            )

            ranked.forEach { row ->
                val qid = row.questionId

                val selectionReason = when {
                    row.duplicate -> SelectionReason.DUPLICATE
                    row.selectedTop3 -> SelectionReason.TOP3_SELECTED
                    else -> SelectionReason.LOW_RANK
                }
                val candidate = QuestionMarketCandidate(
                    batchId = batch.id!!,
                    questionId = qid,
                    category = category,
                    voteCount = row.voteCount,
                    rankInCategory = row.rankInCategory,
                    selectionStatus = if (row.selectedTop3) SelectionStatus.SELECTED_TOP3 else SelectionStatus.NOT_SELECTED,
                    selectionReason = selectionReason,
                    canonicalQuestionId = if (row.duplicate) row.canonicalQuestionId else null,
                    createdAt = now,
                    updatedAt = now,
                )
                allCandidates.add(candidate)
            }

            val selected = ranked.count { it.selectedTop3 }
            logger.info("[MarketBatch#${batch.id}] 카테고리=$category: 후보=${questions.size}개, 선별=${selected}개")
        }

        return candidateRepository.saveAll(allCandidates)
    }

    /**
     * Lifecycle 스케줄러 누락/다운타임으로 VOTING에 남아있는 만료 질문을 배치 시작 시 보정한다.
     * 이후 findBreakReadyForMarketOpenBefore(cutoff)에서 정상 선별된다.
     */
    private fun promoteExpiredVotingToBreak(cutoff: LocalDateTime): Int {
        val staleVoting = questionRepository.findVotingExpiredBefore(cutoff)
        if (staleVoting.isEmpty()) return 0

        var changed = 0
        staleVoting.forEach { q ->
            if (q.status == QuestionStatus.VOTING) {
                q.status = QuestionStatus.BREAK
                changed++
            }
        }
        if (changed > 0) {
            questionRepository.saveAll(staleVoting)
        }
        return changed
    }
}
