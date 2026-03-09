package com.predata.backend

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteSummary
import com.predata.backend.domain.market.BatchStatus
import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.domain.market.SelectionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import com.predata.backend.service.market.MarketBatchService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * MKT-028: 배치 멱등성 테스트
 *
 * 검증:
 * 1. 동일 cutoffSlot 배치 재실행 → 기존 COMPLETED 배치 스킵
 * 2. 질문 중복 오픈 금지
 * 3. forceRun → 재실행 가능 (기존 배치 재사용)
 */
@SpringBootTest
@ActiveProfiles("test")
class MarketOpenIdempotencyTest {

    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var batchRepository: MarketOpenBatchRepository
    @Autowired lateinit var candidateRepository: QuestionMarketCandidateRepository
    @Autowired lateinit var marketPoolRepository: MarketPoolRepository
    @Autowired lateinit var marketBatchService: MarketBatchService

    private val createdQuestionIds = mutableListOf<Long>()
    private val createdCutoffSlots = mutableListOf<LocalDateTime>()

    @AfterEach
    fun tearDown() {
        val batches = batchRepository.findAll()
            .filter { it.cutoffSlotUtc in createdCutoffSlots }
        val batchIds = batches.map { it.id!! }.toSet()

        candidateRepository.findAll()
            .filter { it.batchId in batchIds }
            .forEach { candidateRepository.delete(it) }

        batches.forEach { batchRepository.delete(it) }

        if (createdQuestionIds.isNotEmpty()) {
            marketPoolRepository.findAllByQuestionIds(createdQuestionIds).forEach { marketPoolRepository.delete(it) }
            createdQuestionIds.forEach { qid ->
                voteSummaryRepository.findById(qid).ifPresent { voteSummaryRepository.delete(it) }
            }
            questionRepository.findAllById(createdQuestionIds).forEach { questionRepository.delete(it) }
        }
        createdQuestionIds.clear()
        createdCutoffSlots.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `동일 cutoffSlot 배치 재실행 - COMPLETED 배치는 스킵`() {
        val cutoffSlot = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).minusMinutes(5)
        createdCutoffSlots.add(cutoffSlot)

        saveQuestion("SPORTS", 10, cutoffSlot.minusHours(1))

        // 첫 번째 실행
        marketBatchService.runBatch(cutoffSlot)

        val firstBatch = batchRepository.findByCutoffSlotUtc(cutoffSlot)
        assertThat(firstBatch).isNotNull
        assertThat(firstBatch!!.status).isEqualTo(BatchStatus.COMPLETED)

        // 두 번째 실행 → 스킵
        marketBatchService.runBatch(cutoffSlot)

        val allBatches = batchRepository.findAll().filter { it.cutoffSlotUtc == cutoffSlot }
        assertThat(allBatches).hasSize(1) // 중복 생성 없음
    }

    @Test
    fun `질문 중복 오픈 금지 - 이미 BETTING인 질문 재오픈 시도 시 성공 처리`() {
        val cutoffSlot = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).minusMinutes(10)
        createdCutoffSlots.add(cutoffSlot)

        val q = saveQuestion("SPORTS", 5, cutoffSlot.minusHours(1))

        // 첫 번째 배치 → BETTING으로 전환
        marketBatchService.forceRun(cutoffSlot)
        val q1 = questionRepository.findById(q.id!!).orElseThrow()
        assertThat(q1.status).isEqualTo(QuestionStatus.BETTING)

        assertThat(batchRepository.findByCutoffSlotUtc(cutoffSlot)!!.openedCount).isEqualTo(1)

        // forceRun 재실행: 기존 배치 초기화 후 재처리
        // question이 이미 BETTING → findBreakReadyForMarketOpenBefore 제외 → 후보 없음 → COMPLETED
        marketBatchService.forceRun(cutoffSlot)

        val secondBatch = batchRepository.findByCutoffSlotUtc(cutoffSlot)!!
        assertThat(secondBatch.status).isEqualTo(BatchStatus.COMPLETED)
        // BETTING 질문은 BREAK 후보 조회에서 제외 → 새 오픈 없음
        assertThat(secondBatch.openedCount).isEqualTo(0)

        // MarketPool 중복 생성 없음
        val pools = marketPoolRepository.findAllByQuestionIds(listOf(q.id!!))
        assertThat(pools).hasSizeLessThanOrEqualTo(1)
    }

    @Test
    fun `후보 row UNIQUE 제약 - 동일 배치·질문 중복 저장 시도 시 안전`() {
        val cutoffSlot = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).minusMinutes(15)
        createdCutoffSlots.add(cutoffSlot)

        saveQuestion("SPORTS", 10, cutoffSlot.minusHours(1))
        saveQuestion("SPORTS", 8,  cutoffSlot.minusHours(1))

        marketBatchService.forceRun(cutoffSlot)

        val batch = batchRepository.findByCutoffSlotUtc(cutoffSlot)!!
        val candidates = candidateRepository.findByBatchId(batch.id!!)

        // 중복 없이 각 질문당 1개 후보
        val questionIds = candidates.map { it.questionId }
        assertThat(questionIds).doesNotHaveDuplicates()

        // SELECTED_TOP3 openStatus = OPENED
        val opened = candidates.filter { it.selectionStatus == SelectionStatus.SELECTED_TOP3 }
        assertThat(opened).allMatch { it.openStatus == OpenStatus.OPENED }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveQuestion(category: String, voteCount: Long, votingEndAt: LocalDateTime): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val bettingStartAt = votingEndAt.plusMinutes(5)
        val status = if (bettingStartAt <= now) QuestionStatus.BREAK else QuestionStatus.VOTING
        val q = questionRepository.save(
            Question(
                title          = "멱등성 테스트 [$category]",
                category       = category,
                status         = status,
                votingEndAt    = votingEndAt,
                bettingStartAt = bettingStartAt,
                bettingEndAt   = now.plusHours(2),
                expiredAt      = now.plusHours(3),
            )
        )
        voteSummaryRepository.save(VoteSummary(questionId = q.id!!, totalCount = voteCount))
        createdQuestionIds.add(q.id!!)
        return q
    }
}
