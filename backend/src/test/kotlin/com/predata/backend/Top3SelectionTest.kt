package com.predata.backend

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteSummary
import com.predata.backend.domain.market.MarketOpenBatch
import com.predata.backend.domain.market.SelectionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import com.predata.backend.service.market.CategoryTop3SelectionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * MKT-027: Top3 선별 테스트 (동점 포함)
 *
 * 검증:
 * 1. BREAK 대기(5분 경과) 질문만 후보 수집
 * 2. vote_count DESC → createdAt ASC → question_id ASC 정렬
 * 3. 카테고리별 Top3만 SELECTED_TOP3
 * 4. 나머지는 NOT_SELECTED
 * 5. 동점 시 createdAt ASC로 타이-브레이킹
 */
@SpringBootTest
@ActiveProfiles("test")
class Top3SelectionTest {

    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var batchRepository: MarketOpenBatchRepository
    @Autowired lateinit var candidateRepository: QuestionMarketCandidateRepository
    @Autowired lateinit var selectionService: CategoryTop3SelectionService

    private val createdQuestionIds = mutableListOf<Long>()
    private val createdBatchIds = mutableListOf<Long>()

    @AfterEach
    fun tearDown() {
        if (createdBatchIds.isNotEmpty()) {
            candidateRepository.findAll()
                .filter { it.batchId in createdBatchIds }
                .forEach { candidateRepository.delete(it) }
            createdBatchIds.forEach { batchRepository.deleteById(it) }
        }
        if (createdQuestionIds.isNotEmpty()) {
            createdQuestionIds.forEach { qid ->
                voteSummaryRepository.findById(qid).ifPresent { voteSummaryRepository.delete(it) }
            }
            questionRepository.findAllById(createdQuestionIds).forEach { questionRepository.delete(it) }
        }
        createdQuestionIds.clear()
        createdBatchIds.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `카테고리별 Top3 선별 - 5개 중 상위 3개만 SELECTED_TOP3`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val cutoff = now

        // SPORTS 카테고리 5개 질문 (투표 수 내림차순 예상)
        val q1 = saveQuestion("SPORTS", voteCount = 10, votingEndAt = now.minusHours(1), createdAt = now.minusHours(5))
        val q2 = saveQuestion("SPORTS", voteCount = 8,  votingEndAt = now.minusHours(1), createdAt = now.minusHours(4))
        val q3 = saveQuestion("SPORTS", voteCount = 6,  votingEndAt = now.minusHours(1), createdAt = now.minusHours(3))
        val q4 = saveQuestion("SPORTS", voteCount = 4,  votingEndAt = now.minusHours(1), createdAt = now.minusHours(2))
        val q5 = saveQuestion("SPORTS", voteCount = 2,  votingEndAt = now.minusHours(1), createdAt = now.minusHours(1))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        assertThat(candidates).hasSize(5)

        val byQuestionId = candidates.associateBy { it.questionId }

        assertThat(byQuestionId[q1.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQuestionId[q1.id!!]?.rankInCategory).isEqualTo(1)

        assertThat(byQuestionId[q2.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQuestionId[q2.id!!]?.rankInCategory).isEqualTo(2)

        assertThat(byQuestionId[q3.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQuestionId[q3.id!!]?.rankInCategory).isEqualTo(3)

        assertThat(byQuestionId[q4.id!!]?.selectionStatus).isEqualTo(SelectionStatus.NOT_SELECTED)
        assertThat(byQuestionId[q5.id!!]?.selectionStatus).isEqualTo(SelectionStatus.NOT_SELECTED)
    }

    @Test
    fun `동점 처리 - 투표 수 같을 때 createdAt ASC로 선별`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val cutoff = now

        // 같은 vote_count=6, createdAt 차이로 타이브레이킹
        // 더 일찍 생성된 qEarlier가 rank3, qLater가 rank4 (탈락)
        saveQuestion("TECH", voteCount = 10, votingEndAt = now.minusHours(1), createdAt = now.minusHours(5))
        saveQuestion("TECH", voteCount = 8,  votingEndAt = now.minusHours(1), createdAt = now.minusHours(4))
        val qEarlier = saveQuestion("TECH", voteCount = 6, votingEndAt = now.minusHours(1), createdAt = now.minusHours(3))
        val qLater   = saveQuestion("TECH", voteCount = 6, votingEndAt = now.minusHours(1), createdAt = now.minusHours(2))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        val byQuestionId = candidates.associateBy { it.questionId }

        // qEarlier는 createdAt이 더 이르므로 rank3 (선별됨)
        assertThat(byQuestionId[qEarlier.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQuestionId[qEarlier.id!!]?.rankInCategory).isEqualTo(3)

        // qLater는 rank4 (탈락)
        assertThat(byQuestionId[qLater.id!!]?.selectionStatus).isEqualTo(SelectionStatus.NOT_SELECTED)
        assertThat(byQuestionId[qLater.id!!]?.rankInCategory).isEqualTo(4)
    }

    @Test
    fun `복수 카테고리 - 카테고리 독립적으로 선별`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val cutoff = now

        // SPORTS: 4개 → 상위 3개 선별
        (1..4).forEach { i ->
            saveQuestion("SPORTS", voteCount = (10 - i).toLong(), votingEndAt = now.minusHours(1), createdAt = now.minusHours(i.toLong()))
        }
        // ECONOMY: 2개 → 전부 선별 (< 3)
        (1..2).forEach { i ->
            saveQuestion("ECONOMY", voteCount = (5 - i).toLong(), votingEndAt = now.minusHours(1), createdAt = now.minusHours(i.toLong()))
        }

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        val sportsCandidates  = candidates.filter { it.category == "SPORTS" }
        val economyCandidates = candidates.filter { it.category == "ECONOMY" }

        assertThat(sportsCandidates.count { it.selectionStatus == SelectionStatus.SELECTED_TOP3 }).isEqualTo(3)
        assertThat(sportsCandidates.count { it.selectionStatus == SelectionStatus.NOT_SELECTED  }).isEqualTo(1)

        assertThat(economyCandidates.count { it.selectionStatus == SelectionStatus.SELECTED_TOP3 }).isEqualTo(2)
        assertThat(economyCandidates.count { it.selectionStatus == SelectionStatus.NOT_SELECTED  }).isEqualTo(0)
    }

    @Test
    fun `미래 투표 종료 질문은 후보에서 제외`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val cutoff = now

        val expired  = saveQuestion("POLITICS", voteCount = 5, votingEndAt = now.minusHours(1), createdAt = now.minusHours(2))
        // votingEndAt이 미래 → 선별 대상 아님
        saveQuestion("POLITICS", voteCount = 100, votingEndAt = now.plusHours(1), createdAt = now.minusHours(2))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        // 만료된 질문만 후보에 포함
        assertThat(candidates.map { it.questionId }).containsExactly(expired.id!!)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveQuestion(
        category: String,
        voteCount: Long,
        votingEndAt: LocalDateTime,
        createdAt: LocalDateTime,
    ): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val bettingStartAt = votingEndAt.plusMinutes(5)
        val status = if (bettingStartAt <= now) QuestionStatus.BREAK else QuestionStatus.VOTING
        val q = questionRepository.save(
            Question(
                title        = "Top3 테스트 [$category]",
                category     = category,
                status       = status,
                votingEndAt  = votingEndAt,
                bettingStartAt = bettingStartAt,
                bettingEndAt   = now.plusHours(2),
                expiredAt      = now.plusHours(3),
                createdAt      = createdAt,
            )
        )
        voteSummaryRepository.save(
            VoteSummary(questionId = q.id!!, totalCount = voteCount)
        )
        createdQuestionIds.add(q.id!!)
        return q
    }

    private fun saveBatch(cutoff: LocalDateTime): MarketOpenBatch {
        val batch = batchRepository.save(
            MarketOpenBatch(
                cutoffSlotUtc = cutoff,
                startedAt     = LocalDateTime.now(ZoneOffset.UTC),
            )
        )
        createdBatchIds.add(batch.id!!)
        return batch
    }
}
