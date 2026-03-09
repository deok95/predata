package com.predata.backend

import com.fasterxml.jackson.databind.JsonNode
import com.predata.backend.config.JwtUtil
import com.predata.backend.domain.DailyVoteUsage
import com.predata.backend.domain.Member
import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.OnChainVoteRelay
import com.predata.backend.repository.DailyVoteUsageRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.OnChainVoteRelayRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VoteOpsAdminApiTest {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jwtUtil: JwtUtil
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var dailyVoteUsageRepository: DailyVoteUsageRepository
    @Autowired lateinit var onChainVoteRelayRepository: OnChainVoteRelayRepository

    private lateinit var admin: Member
    private lateinit var user: Member
    private lateinit var adminToken: String
    private val usageDate: LocalDate = LocalDate.of(2026, 2, 23)

    @BeforeEach
    fun setUp() {
        admin = memberRepository.save(
            Member(email = "voteops-admin-${UUID.randomUUID()}@test.com", countryCode = "KR", role = "ADMIN")
        )
        user = memberRepository.save(
            Member(email = "voteops-user-${UUID.randomUUID()}@test.com", countryCode = "KR", role = "USER")
        )
        adminToken = jwtUtil.generateToken(admin.id!!, admin.email, "ADMIN")

        dailyVoteUsageRepository.save(DailyVoteUsage(memberId = user.id!!, usageDate = usageDate, usedCount = 4))
        dailyVoteUsageRepository.save(DailyVoteUsage(memberId = admin.id!!, usageDate = usageDate, usedCount = 1))

        onChainVoteRelayRepository.save(
            OnChainVoteRelay(
                voteId = 91001L,
                memberId = user.id!!,
                questionId = 1111L,
                choice = com.predata.backend.domain.Choice.YES,
                status = OnChainRelayStatus.PENDING,
                retryCount = 0,
            )
        )
        onChainVoteRelayRepository.save(
            OnChainVoteRelay(
                voteId = 91002L,
                memberId = admin.id!!,
                questionId = 2222L,
                choice = com.predata.backend.domain.Choice.NO,
                status = OnChainRelayStatus.FAILED,
                retryCount = 3,
            )
        )
    }

    @AfterEach
    fun tearDown() {
        onChainVoteRelayRepository.findAll()
            .filter { it.voteId in setOf(91001L, 91002L) }
            .forEach { onChainVoteRelayRepository.delete(it) }

        dailyVoteUsageRepository.findAll()
            .filter { it.usageDate == usageDate && it.memberId in setOf(admin.id, user.id) }
            .forEach { dailyVoteUsageRepository.delete(it) }

        memberRepository.findById(user.id!!).ifPresent { memberRepository.delete(it) }
        memberRepository.findById(admin.id!!).ifPresent { memberRepository.delete(it) }
    }

    @Test
    fun `인증 없이 vote ops API 호출 시 401`() {
        val response = restTemplate.getForEntity("/api/admin/vote-ops/usage", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `usage 조회는 page size sortBy sortDir를 지원한다`() {
        val response = get("/api/admin/vote-ops/usage?date=2026-02-23&page=0&size=1&sortBy=usedCount&sortDir=desc")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = data(response)
        assertThat(data.path("totalMembers").asInt()).isGreaterThanOrEqualTo(2)
        assertThat(data.path("page").asInt()).isEqualTo(0)
        assertThat(data.path("size").asInt()).isEqualTo(1)
        assertThat(data.path("entries").size()).isEqualTo(1)
        assertThat(data.path("entries")[0].path("usedCount").asInt()).isGreaterThanOrEqualTo(1)
    }

    @Test
    fun `relay 조회는 status 필터와 page size sortBy sortDir를 지원한다`() {
        val response = get("/api/admin/vote-ops/relay?status=FAILED&page=0&size=10&sortBy=retryCount&sortDir=desc")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = data(response)
        assertThat(data.path("total").asInt()).isGreaterThanOrEqualTo(1)
        assertThat(data.path("items").size()).isGreaterThanOrEqualTo(1)
        assertThat(data.path("items")[0].path("status").asText()).isEqualTo("FAILED")
    }

    private fun authHeaders() = HttpHeaders().apply {
        set(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
    }

    private fun get(path: String): ResponseEntity<JsonNode> =
        restTemplate.exchange(path, HttpMethod.GET, HttpEntity<Void>(authHeaders()), JsonNode::class.java)

    private fun data(response: ResponseEntity<JsonNode>): JsonNode = response.body!!.path("data")
}
