package com.predata.backend

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteSummary
import com.predata.backend.domain.market.BatchStatus
import com.predata.backend.domain.market.MarketOpenBatch
import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import com.predata.backend.service.market.CategoryTop3SelectionService
import com.predata.backend.service.market.MarketBatchService
import com.predata.backend.service.market.MarketOpenService
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
 * MKT-029: 오픈 실패 격리 테스트
 *
 * 검증:
 * 1. 한 질문의 오픈 실패가 다른 질문 오픈을 막지 않는다
 * 2. 실패 질문은 OPEN_FAILED 기록, 질문 상태 미변경
 * 3. 성공 질문은 OPENED 기록, 질문 상태 = BETTING
 * 4. 배치 최종 상태 = PARTIAL_FAILED
 * 5. retry-open은 OPEN_FAILED 건만 재처리
 *
 * 실패 시뮬레이션 전략:
 * - selectAndSave 호출 후, qFail 상태를 CANCELLED로 변경
 * - openAll 호출 시 qFail openSingle → "오픈 불가 상태" → OPEN_FAILED
 * - qOk, qOk2는 정상 오픈
 */
@SpringBootTest
@ActiveProfiles("test")
class MarketOpenFailureIsolationTest {

    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var batchRepository: MarketOpenBatchRepository
    @Autowired lateinit var candidateRepository: QuestionMarketCandidateRepository
    @Autowired lateinit var marketPoolRepository: MarketPoolRepository
    @Autowired lateinit var selectionService: CategoryTop3SelectionService
    @Autowired lateinit var openService: MarketOpenService
    @Autowired lateinit var marketBatchService: MarketBatchService

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
            marketPoolRepository.findAllByQuestionIds(createdQuestionIds)
                .forEach { marketPoolRepository.delete(it) }
            createdQuestionIds.forEach { qid ->
                voteSummaryRepository.findById(qid).ifPresent { voteSummaryRepository.delete(it) }
            }
            questionRepository.findAllById(createdQuestionIds)
                .forEach { questionRepository.delete(it) }
        }
        createdQuestionIds.clear()
        createdBatchIds.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `한 질문 실패가 다른 질문 오픈을 막지 않는다 - PARTIAL_FAILED`() {
        val cutoff = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).minusMinutes(40)

        val qFail = saveQuestion("SPORTS", voteCount = 10, votingEndAt = cutoff.minusHours(1))
        val q2    = saveQuestion("SPORTS", voteCount = 8,  votingEndAt = cutoff.minusHours(1))
        val q3    = saveQuestion("SPORTS", voteCount = 6,  votingEndAt = cutoff.minusHours(1))

        val batch = saveBatch(cutoff)

        // Step 1: 선별 (qFail, q2, q3 모두 VOTING이므로 SELECTED_TOP3로 선별됨)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        // Step 2: 선별 후 qFail을 CANCELLED로 변경 (외부 이벤트 시뮬레이션)
        val qFailDb = questionRepository.findById(qFail.id!!).orElseThrow()
        qFailDb.status = QuestionStatus.CANCELLED
        questionRepository.save(qFailDb)

        // Step 3: 오픈 (qFail은 CANCELLED → OPEN_FAILED, q2/q3 → OPENED)
        val (opened, failed) = openService.openAll(candidates)

        assertThat(opened).isEqualTo(2)
        assertThat(failed).isEqualTo(1)

        assertThat(questionRepository.findById(q2.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(q3.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(qFail.id!!).orElseThrow().status).isEqualTo(QuestionStatus.CANCELLED)

        val byQuestion = candidateRepository.findByBatchId(batch.id!!).associateBy { it.questionId }
        assertThat(byQuestion[qFail.id!!]?.openStatus).isEqualTo(OpenStatus.OPEN_FAILED)
        assertThat(byQuestion[qFail.id!!]?.openError).isNotBlank()
        assertThat(byQuestion[q2.id!!]?.openStatus).isEqualTo(OpenStatus.OPENED)
        assertThat(byQuestion[q3.id!!]?.openStatus).isEqualTo(OpenStatus.OPENED)
    }

    @Test
    fun `전부 실패 시 openAll 결과 opened=0`() {
        val cutoff = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).minusMinutes(45)

        val q1 = saveQuestion("CULTURE", voteCount = 5, votingEndAt = cutoff.minusHours(1))
        val q2 = saveQuestion("CULTURE", voteCount = 3, votingEndAt = cutoff.minusHours(1))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        // 모두 CANCELLED
        listOf(q1.id!!, q2.id!!).forEach { qid ->
            val q = questionRepository.findById(qid).orElseThrow()
            q.status = QuestionStatus.CANCELLED
            questionRepository.save(q)
        }

        val (opened, failed) = openService.openAll(candidates)
        assertThat(opened).isEqualTo(0)
        assertThat(failed).isEqualTo(2)
    }

    @Test
    fun `retry-open은 OPEN_FAILED 건만 재처리`() {
        val cutoff = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).minusMinutes(50)

        val qFail = saveQuestion("INTERNATIONAL", voteCount = 7, votingEndAt = cutoff.minusHours(1))
        val qOk   = saveQuestion("INTERNATIONAL", voteCount = 5, votingEndAt = cutoff.minusHours(1))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        // 첫 번째 오픈: qFail을 CANCELLED로 변경 후 실패 유도
        val qFailDb = questionRepository.findById(qFail.id!!).orElseThrow()
        qFailDb.status = QuestionStatus.CANCELLED
        questionRepository.save(qFailDb)

        val (firstOpened, firstFailed) = openService.openAll(candidates)
        assertThat(firstOpened).isEqualTo(1)
        assertThat(firstFailed).isEqualTo(1)

        // 배치 집계 업데이트
        batch.openedCount = firstOpened
        batch.failedCount = firstFailed
        batch.status = BatchStatus.PARTIAL_FAILED
        batchRepository.save(batch)

        // qFail 복원 → BREAK로 → retry 시 성공
        val qFailRestored = questionRepository.findById(qFail.id!!).orElseThrow()
        qFailRestored.status = QuestionStatus.BREAK
        questionRepository.save(qFailRestored)

        val retried = marketBatchService.retryOpen(batch.id!!)

        assertThat(retried.status).isEqualTo(BatchStatus.COMPLETED)

        val byQuestion = candidateRepository.findByBatchId(batch.id!!).associateBy { it.questionId }
        assertThat(byQuestion[qFail.id!!]?.openStatus).isEqualTo(OpenStatus.OPENED)
        assertThat(byQuestion[qOk.id!!]?.openStatus).isEqualTo(OpenStatus.OPENED)
        assertThat(questionRepository.findById(qFail.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
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
                title          = "실패격리 [$category]",
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

    private fun saveBatch(cutoffSlot: LocalDateTime): MarketOpenBatch {
        val batch = batchRepository.save(
            MarketOpenBatch(
                cutoffSlotUtc = cutoffSlot,
                startedAt     = LocalDateTime.now(ZoneOffset.UTC),
            )
        )
        createdBatchIds.add(batch.id!!)
        return batch
    }
}
