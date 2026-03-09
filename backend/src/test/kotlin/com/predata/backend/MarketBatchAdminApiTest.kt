package com.predata.backend

import com.fasterxml.jackson.databind.JsonNode
import com.predata.backend.config.JwtUtil
import com.predata.backend.domain.Member
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteSummary
import com.predata.backend.domain.market.OpenStatus
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * MKT-030: 관리자 API HTTP E2E 테스트
 *
 * 검증 (HTTP 계층 포함):
 * 1. POST /api/admin/markets/batches/{cutoffSlot}/run — 수동 실행, 200 OK
 * 2. GET  /api/admin/markets/batches                  — 배치 목록 조회, 200 OK
 * 3. GET  /api/admin/markets/batches/{id}/candidates  — 후보 상세 조회, 200 OK
 * 4. GET  /api/admin/markets/batches/{id}/summary     — 집계 요약, 200 OK
 * 5. POST /api/admin/markets/batches/{id}/retry-open  — 409 (실패 후보 없음)
 * 6. GET  /api/admin/markets/batches/999999/candidates — 404 (없는 배치)
 * 7. GET  /api/admin/markets/batches (인증 없음) — 401
 * 8. POST /api/admin/markets/batches/invalid/run — 400 (잘못된 형식)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MarketBatchAdminApiTest {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jwtUtil: JwtUtil

    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var voteSummaryRepository: VoteSummaryRepository
    @Autowired lateinit var batchRepository: MarketOpenBatchRepository
    @Autowired lateinit var candidateRepository: QuestionMarketCandidateRepository
    @Autowired lateinit var marketPoolRepository: MarketPoolRepository

    private lateinit var adminToken: String
    private lateinit var adminMember: Member

    private val createdQuestionIds = mutableListOf<Long>()
    private val createdCutoffSlots = mutableListOf<LocalDateTime>()

    @BeforeEach
    fun setUp() {
        adminMember = memberRepository.save(
            Member(email = "mkt-admin-${UUID.randomUUID()}@test.com", countryCode = "KR", role = "ADMIN")
        )
        adminToken = jwtUtil.generateToken(adminMember.id!!, adminMember.email, "ADMIN")
    }

    @AfterEach
    fun tearDown() {
        val batches = batchRepository.findAll().filter { it.cutoffSlotUtc in createdCutoffSlots }
        val batchIds = batches.map { it.id!! }.toSet()

        candidateRepository.findAll()
            .filter { it.batchId in batchIds }
            .forEach { candidateRepository.delete(it) }
        batches.forEach { batchRepository.delete(it) }

        if (createdQuestionIds.isNotEmpty()) {
            marketPoolRepository.findAllByQuestionIds(createdQuestionIds)
                .forEach { marketPoolRepository.delete(it) }
            createdQuestionIds.forEach { qid ->
                voteSummaryRepository.findById(qid).ifPresent { voteSummaryRepository.delete(it) }
            }
            questionRepository.findAllById(createdQuestionIds)
                .forEach { questionRepository.delete(it) }
        }

        memberRepository.delete(adminMember)
        createdQuestionIds.clear()
        createdCutoffSlots.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `인증 없이 admin API 호출 시 401`() {
        val response = restTemplate.getForEntity("/api/admin/markets/batches", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `forceRun - 200 OK, 배치 COMPLETED, openedCount 정확`() {
        val cutoff = slot()
        saveQuestion("SPORTS", 10, cutoff.minusHours(1))
        saveQuestion("SPORTS", 8,  cutoff.minusHours(1))

        val response = post("/api/admin/markets/batches/$cutoff/run")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = data(response)
        assertThat(data.path("status").asText()).isEqualTo("COMPLETED")
        assertThat(data.path("openedCount").asInt()).isEqualTo(2)
        assertThat(data.path("failedCount").asInt()).isEqualTo(0)
    }

    @Test
    fun `배치 목록 조회 - 200 OK, 생성 배치 포함`() {
        val cutoff = slot()
        saveQuestion("ECONOMY", 5, cutoff.minusHours(1))
        post("/api/admin/markets/batches/$cutoff/run")

        val response = get("/api/admin/markets/batches")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val items = data(response)
        assertThat(items.isArray).isTrue()
        val cutoffPrefix = cutoff.toString().substring(0, 16) // "2026-02-23T01:23"
        val found = items.any { it.path("cutoffSlotUtc").asText().startsWith(cutoffPrefix) }
        assertThat(found).isTrue()
    }

    @Test
    fun `배치 목록 조회는 page size sortBy sortDir를 지원한다`() {
        val cutoff = slot()
        saveQuestion("ECONOMY", 5, cutoff.minusHours(1))
        saveQuestion("ECONOMY", 3, cutoff.minusHours(1))
        post("/api/admin/markets/batches/$cutoff/run")

        val response = get("/api/admin/markets/batches?page=0&size=1&sortBy=openedCount&sortDir=desc")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val items = data(response)
        assertThat(items.isArray).isTrue()
        assertThat(items.size()).isEqualTo(1)
    }

    @Test
    fun `후보 목록 조회 - 200 OK, 후보 4개 (OPENED=3 미처리=1)`() {
        val cutoff = slot()
        saveQuestion("TECH", 15, cutoff.minusHours(1))
        saveQuestion("TECH", 10, cutoff.minusHours(1))
        saveQuestion("TECH", 5,  cutoff.minusHours(1))
        saveQuestion("TECH", 2,  cutoff.minusHours(1)) // NOT_SELECTED

        val runResp = post("/api/admin/markets/batches/$cutoff/run")
        val batchId = data(runResp).path("id").asLong()

        val response = get("/api/admin/markets/batches/$batchId/candidates")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val candidates = data(response)
        assertThat(candidates.size()).isEqualTo(4)
        val openedCount = candidates.count { it.path("openStatus").asText() == OpenStatus.OPENED.name }
        assertThat(openedCount).isEqualTo(3)
    }

    @Test
    fun `후보 목록 조회는 page size sortBy sortDir를 지원한다`() {
        val cutoff = slot()
        saveQuestion("TECH", 15, cutoff.minusHours(1))
        saveQuestion("TECH", 10, cutoff.minusHours(1))
        saveQuestion("TECH", 5,  cutoff.minusHours(1))

        val runResp = post("/api/admin/markets/batches/$cutoff/run")
        val batchId = data(runResp).path("id").asLong()

        val response = get("/api/admin/markets/batches/$batchId/candidates?page=0&size=2&sortBy=voteCount&sortDir=desc")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val candidates = data(response)
        assertThat(candidates.size()).isEqualTo(2)
        val firstVote = candidates[0].path("voteCount").asLong()
        val secondVote = candidates[1].path("voteCount").asLong()
        assertThat(firstVote).isGreaterThanOrEqualTo(secondVote)
    }

    @Test
    fun `집계 요약 - 200 OK, selectedCount openedCount totalCandidates 정확`() {
        val cutoff = slot()
        saveQuestion("CULTURE", 8, cutoff.minusHours(1))
        saveQuestion("CULTURE", 6, cutoff.minusHours(1))

        val runResp = post("/api/admin/markets/batches/$cutoff/run")
        val batchId = data(runResp).path("id").asLong()

        val response = get("/api/admin/markets/batches/$batchId/summary")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val summary = data(response)
        assertThat(summary.path("selectedCount").asInt()).isEqualTo(2)
        assertThat(summary.path("openedCount").asInt()).isEqualTo(2)
        assertThat(summary.path("totalCandidates").asInt()).isEqualTo(2)
    }

    @Test
    fun `없는 배치 ID 후보 조회 시 404`() {
        val response = get("/api/admin/markets/batches/999999/candidates")
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `실패 후보 없을 때 retry-open 시 409`() {
        val cutoff = slot()
        saveQuestion("POLITICS", 10, cutoff.minusHours(1))

        val runResp = post("/api/admin/markets/batches/$cutoff/run")
        val batchId = data(runResp).path("id").asLong()

        val response = post("/api/admin/markets/batches/$batchId/retry-open")
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `cutoffSlot 형식 오류 시 400`() {
        val response = post("/api/admin/markets/batches/invalid-slot/run")
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun authHeaders() = HttpHeaders().apply {
        set(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
    }

    private fun get(path: String): ResponseEntity<JsonNode> =
        restTemplate.exchange(path, HttpMethod.GET, HttpEntity<Void>(authHeaders()), JsonNode::class.java)

    private fun post(path: String): ResponseEntity<JsonNode> =
        restTemplate.exchange(path, HttpMethod.POST, HttpEntity<Void>(authHeaders()), JsonNode::class.java)

    private fun data(response: ResponseEntity<JsonNode>): JsonNode =
        response.body!!.path("data")

    // ─────────────────────────────────────────────────────────────────────────
    // Data helpers
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
                title          = "AdminApiTest [$category]",
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
