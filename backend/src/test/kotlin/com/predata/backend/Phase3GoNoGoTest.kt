package com.predata.backend

import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteSummary
import com.predata.backend.domain.market.BatchStatus
import com.predata.backend.domain.market.MarketOpenBatch
import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.domain.market.SelectionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import com.predata.backend.service.market.CategoryTop3SelectionService
import com.predata.backend.service.market.MarketBatchScheduler
import com.predata.backend.service.market.MarketBatchService
import com.predata.backend.service.market.MarketOpenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * MKT-031: Phase 3 Go/No-Go 수용 기준 검증
 *
 * GNG-01: BREAK 대기(5분 경과) 질문이 배치 후보로 잡힌다.
 * GNG-02: 카테고리별 Top3가 정렬 규칙대로 확정된다.
 * GNG-03: Top3만 BETTING 전환된다. (Not-selected는 BREAK 유지)
 * GNG-04: seedPool 성공 후 MarketPool 생성 + BETTING 전환 확인.
 * GNG-05: 실패 격리 아키텍처 확인 (openSingle REQUIRES_NEW).
 * GNG-06: 운영 빈 등록 확인 (배치/후보/API 컨트롤러).
 * GNG-07: retry-open은 실패 건만 재처리한다.
 * GNG-08: 전체 기준 통과 → Phase 3 Go.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName::class)
class Phase3GoNoGoTest {

    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var batchRepository: MarketOpenBatchRepository
    @Autowired lateinit var candidateRepository: QuestionMarketCandidateRepository
    @Autowired lateinit var marketPoolRepository: MarketPoolRepository
    @Autowired lateinit var selectionService: CategoryTop3SelectionService
    @Autowired lateinit var openService: MarketOpenService
    @Autowired lateinit var marketBatchService: MarketBatchService
    @Autowired lateinit var appContext: ApplicationContext

    private val createdQuestionIds = mutableListOf<Long>()
    private val createdBatchIds = mutableListOf<Long>()
    private val createdCutoffSlots = mutableListOf<LocalDateTime>()

    @AfterEach
    fun tearDown() {
        // forceRun으로 생성된 배치도 cutoffSlot으로 조회하여 정리
        val slotBatches = batchRepository.findAll()
            .filter { it.cutoffSlotUtc in createdCutoffSlots }
        val allBatchIds = (createdBatchIds + slotBatches.map { it.id!! }).toSet()

        candidateRepository.findAll()
            .filter { it.batchId in allBatchIds }
            .forEach { candidateRepository.delete(it) }
        allBatchIds.forEach { batchRepository.deleteById(it) }

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
        createdCutoffSlots.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GNG-01 votingEndAt 지난 질문이 배치 후보로 잡힌다`() {
        val cutoff = slot()
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val expired    = saveQuestion("SPORTS", 5, cutoff.minusHours(1))
        val notExpired = saveQuestion("SPORTS", 10, now.plusHours(1)) // 미래 종료 (cutoff 기준 미래)

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        assertThat(candidates.map { it.questionId }).contains(expired.id!!)
        assertThat(candidates.map { it.questionId }).doesNotContain(notExpired.id!!)
    }

    @Test
    fun `GNG-02 카테고리별 Top3가 정렬 규칙대로 확정된다`() {
        val cutoff = slot()

        val q1 = saveQuestion("TECH", 100, cutoff.minusHours(1))
        val q2 = saveQuestion("TECH", 80,  cutoff.minusHours(1))
        val q3 = saveQuestion("TECH", 60,  cutoff.minusHours(1))
        val q4 = saveQuestion("TECH", 20,  cutoff.minusHours(1)) // 탈락

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)
        val byQ = candidates.associateBy { it.questionId }

        assertThat(byQ[q1.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQ[q1.id!!]?.rankInCategory).isEqualTo(1)
        assertThat(byQ[q2.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQ[q3.id!!]?.selectionStatus).isEqualTo(SelectionStatus.SELECTED_TOP3)
        assertThat(byQ[q4.id!!]?.selectionStatus).isEqualTo(SelectionStatus.NOT_SELECTED)
    }

    @Test
    fun `GNG-03 Top3만 BETTING 전환된다 Not-selected는 VOTING 유지`() {
        val cutoff = slot()
        val q1    = saveQuestion("ECONOMY", 50, cutoff.minusHours(1))
        val q2    = saveQuestion("ECONOMY", 40, cutoff.minusHours(1))
        val q3    = saveQuestion("ECONOMY", 30, cutoff.minusHours(1))
        val rank4 = saveQuestion("ECONOMY", 10, cutoff.minusHours(1))

        marketBatchService.forceRun(cutoff)

        assertThat(questionRepository.findById(q1.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(q2.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(q3.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(rank4.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BREAK)
    }

    @Test
    fun `GNG-04 seedPool 성공 후 MarketPool 생성 및 BETTING 전환`() {
        val cutoff = slot()
        val q1 = saveQuestion("POLITICS", 15, cutoff.minusHours(1))
        val q2 = saveQuestion("POLITICS", 10, cutoff.minusHours(1))

        marketBatchService.forceRun(cutoff)

        // MarketPool 생성 확인
        val pools = marketPoolRepository.findAllByQuestionIds(listOf(q1.id!!, q2.id!!))
        assertThat(pools).hasSize(2)
        assertThat(pools.map { it.questionId }).containsExactlyInAnyOrder(q1.id!!, q2.id!!)

        // 질문 상태 BETTING 확인
        assertThat(questionRepository.findById(q1.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(q2.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)

        val batch = batchRepository.findAll().filter { it.cutoffSlotUtc == cutoff }.firstOrNull()
        assertThat(batch?.openedCount).isEqualTo(2)
    }

    @Test
    fun `GNG-05 실패 격리 - 선별 후 상태 변경된 질문은 OPEN_FAILED 다른 질문은 오픈`() {
        val cutoff = slot()
        val qFail = saveQuestion("GENERAL", 10, cutoff.minusHours(1))
        val q2    = saveQuestion("GENERAL", 8,  cutoff.minusHours(1))
        val q3    = saveQuestion("GENERAL", 6,  cutoff.minusHours(1))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        // 선별 후 qFail 상태 변경 → openSingle 실패 유도
        val qFailDb = questionRepository.findById(qFail.id!!).orElseThrow()
        qFailDb.status = QuestionStatus.CANCELLED
        questionRepository.save(qFailDb)

        val (opened, failed) = openService.openAll(candidates)
        assertThat(opened).isEqualTo(2)
        assertThat(failed).isEqualTo(1)

        assertThat(questionRepository.findById(q2.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(q3.id!!).orElseThrow().status).isEqualTo(QuestionStatus.BETTING)
        assertThat(questionRepository.findById(qFail.id!!).orElseThrow().status).isEqualTo(QuestionStatus.CANCELLED)
    }

    @Test
    fun `GNG-06 운영 빈 등록 확인`() {
        assertThat(appContext.containsBean("marketBatchService")).isTrue()
        assertThat(appContext.containsBean("marketBatchScheduler")).isTrue()
        assertThat(appContext.containsBean("categoryTop3SelectionService")).isTrue()
        assertThat(appContext.containsBean("marketOpenService")).isTrue()
        assertThat(appContext.containsBean("marketBatchAdminController")).isTrue()
        assertThat(appContext.containsBean("marketOpenBatchRepository")).isTrue()
        assertThat(appContext.containsBean("questionMarketCandidateRepository")).isTrue()
        assertThat(appContext.containsBean("marketCategoryRepository")).isTrue()
    }

    @Test
    fun `GNG-07 retry-open은 OPEN_FAILED 건만 재처리`() {
        val cutoff = slot()
        val qFail = saveQuestion("CULTURE", 15, cutoff.minusHours(1))
        val qOk   = saveQuestion("CULTURE", 10, cutoff.minusHours(1))

        val batch = saveBatch(cutoff)
        val candidates = selectionService.selectAndSave(batch, cutoff)

        val qFailDb = questionRepository.findById(qFail.id!!).orElseThrow()
        qFailDb.status = QuestionStatus.CANCELLED
        questionRepository.save(qFailDb)

        val (firstOpened, firstFailed) = openService.openAll(candidates)
        batch.openedCount = firstOpened
        batch.failedCount = firstFailed
        batch.status = BatchStatus.PARTIAL_FAILED
        batchRepository.save(batch)

        // qFail 복원 후 retry
        val qFailRestored = questionRepository.findById(qFail.id!!).orElseThrow()
        qFailRestored.status = QuestionStatus.BREAK
        questionRepository.save(qFailRestored)

        val retried = marketBatchService.retryOpen(batch.id!!)
        assertThat(retried.status).isEqualTo(BatchStatus.COMPLETED)

        val byQ = candidateRepository.findByBatchId(batch.id!!).associateBy { it.questionId }
        assertThat(byQ[qFail.id!!]?.openStatus).isEqualTo(OpenStatus.OPENED)
        assertThat(byQ[qOk.id!!]?.openStatus).isEqualTo(OpenStatus.OPENED)
    }

    @Test
    fun `GNG-08 Phase3 Go - 핵심 서비스 빈 의존성 최종 확인`() {
        assertThat(appContext.getBean(MarketBatchService::class.java)).isNotNull
        assertThat(appContext.getBean(MarketBatchScheduler::class.java)).isNotNull
        assertThat(appContext.getBean(MarketOpenService::class.java)).isNotNull
        assertThat(appContext.getBean(CategoryTop3SelectionService::class.java)).isNotNull
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun slot(): LocalDateTime {
        val s = LocalDateTime.now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.MINUTES)
            .minusMinutes((60 + (Math.random() * 900).toLong()))
        createdCutoffSlots.add(s)
        return s
    }

    private fun saveQuestion(category: String, voteCount: Long, votingEndAt: LocalDateTime): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val bettingStartAt = votingEndAt.plusMinutes(5)
        val status = if (bettingStartAt <= now) QuestionStatus.BREAK else QuestionStatus.VOTING
        val q = questionRepository.save(
            Question(
                title          = "GoNoGo [$category]",
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
