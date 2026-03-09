package com.predata.backend

import com.fasterxml.jackson.databind.JsonNode
import com.predata.backend.config.JwtUtil
import com.predata.backend.domain.Member
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.MemberWalletRepository
import com.predata.backend.repository.TreasuryLedgerRepository
import com.predata.backend.repository.WalletLedgerRepository
import com.predata.backend.service.WalletBalanceService
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
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FinanceAdminApiTest {

    @Autowired lateinit var restTemplate: TestRestTemplate
    @Autowired lateinit var jwtUtil: JwtUtil
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var memberWalletRepository: MemberWalletRepository
    @Autowired lateinit var walletLedgerRepository: WalletLedgerRepository
    @Autowired lateinit var treasuryLedgerRepository: TreasuryLedgerRepository
    @Autowired lateinit var walletBalanceService: WalletBalanceService

    private lateinit var admin: Member
    private lateinit var user: Member
    private lateinit var adminToken: String

    @BeforeEach
    fun setUp() {
        admin = memberRepository.save(
            Member(email = "finance-admin-${UUID.randomUUID()}@test.com", countryCode = "KR", role = "ADMIN")
        )
        user = memberRepository.save(
            Member(email = "finance-user-${UUID.randomUUID()}@test.com", countryCode = "KR", role = "USER")
        )
        adminToken = jwtUtil.generateToken(admin.id!!, admin.email, "ADMIN")

        walletBalanceService.credit(
            memberId = user.id!!,
            amount = BigDecimal("100.000000"),
            txType = "FIN_TEST_DEPOSIT",
            referenceType = "TEST",
            referenceId = 1L,
            treasuryInflow = true,
            description = "finance admin api test deposit",
        )
        walletBalanceService.debit(
            memberId = user.id!!,
            amount = BigDecimal("30.000000"),
            txType = "FIN_TEST_BET",
            referenceType = "TEST",
            referenceId = 2L,
            description = "finance admin api test debit",
        )
        walletBalanceService.recordTreasuryOutflow(
            amount = BigDecimal("5.000000"),
            txType = "FIN_TEST_WITHDRAW",
            referenceType = "TEST",
            referenceId = 3L,
            description = "finance admin api test outflow",
        )
    }

    @AfterEach
    fun tearDown() {
        walletLedgerRepository.findAll()
            .filter { it.memberId == user.id }
            .forEach { walletLedgerRepository.delete(it) }

        treasuryLedgerRepository.findAll()
            .filter { it.txType.startsWith("FIN_TEST_") }
            .forEach { treasuryLedgerRepository.delete(it) }

        memberWalletRepository.findByMemberId(user.id!!)?.let { memberWalletRepository.delete(it) }
        memberWalletRepository.findByMemberId(admin.id!!)?.let { memberWalletRepository.delete(it) }

        memberRepository.findById(user.id!!).ifPresent { memberRepository.delete(it) }
        memberRepository.findById(admin.id!!).ifPresent { memberRepository.delete(it) }
    }

    @Test
    fun `인증 없이 finance admin API 호출 시 401`() {
        val response = restTemplate.getForEntity("/api/admin/finance/wallets/${user.id}", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `wallet 조회 시 현재 잔고를 반환한다`() {
        val response = get("/api/admin/finance/wallets/${user.id}")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val data = data(response)
        assertThat(data.path("memberId").asLong()).isEqualTo(user.id!!)
        assertThat(data.path("availableBalance").decimalValue()).isEqualByComparingTo("70.000000")
    }

    @Test
    fun `wallet ledger 조회 시 입출금 기록을 반환한다`() {
        val response = get("/api/admin/finance/wallet-ledgers?memberId=${user.id}&page=0&size=20")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val data = data(response)
        assertThat(data.path("totalElements").asLong()).isGreaterThanOrEqualTo(2)
        assertThat(data.path("items").isArray).isTrue()
        val txTypes = data.path("items").map { it.path("txType").asText() }.toSet()
        assertThat(txTypes).contains("FIN_TEST_DEPOSIT", "FIN_TEST_BET")
    }

    @Test
    fun `wallet ledger 조회는 sortDir asc desc를 지원한다`() {
        val desc = data(get("/api/admin/finance/wallet-ledgers?memberId=${user.id}&page=0&size=20&sortDir=desc")).path("items")
        val asc = data(get("/api/admin/finance/wallet-ledgers?memberId=${user.id}&page=0&size=20&sortDir=asc")).path("items")

        assertThat(desc.size()).isGreaterThanOrEqualTo(2)
        assertThat(asc.size()).isEqualTo(desc.size())
        val descFirst = desc.first().path("createdAt").asText()
        val descLast = desc.last().path("createdAt").asText()
        val ascFirst = asc.first().path("createdAt").asText()
        val ascLast = asc.last().path("createdAt").asText()
        assertThat(descFirst >= descLast).isTrue()
        assertThat(ascFirst <= ascLast).isTrue()
    }

    @Test
    fun `wallet ledger 조회는 sortBy amount를 지원한다`() {
        val asc = data(
            get("/api/admin/finance/wallet-ledgers?memberId=${user.id}&page=0&size=20&sortBy=amount&sortDir=asc")
        ).path("items")
        val desc = data(
            get("/api/admin/finance/wallet-ledgers?memberId=${user.id}&page=0&size=20&sortBy=amount&sortDir=desc")
        ).path("items")

        assertThat(asc.size()).isGreaterThanOrEqualTo(2)
        assertThat(desc.size()).isGreaterThanOrEqualTo(2)
        assertThat(asc.first().path("amount").decimalValue() <= asc.last().path("amount").decimalValue()).isTrue()
        assertThat(desc.first().path("amount").decimalValue() >= desc.last().path("amount").decimalValue()).isTrue()
    }

    @Test
    fun `treasury ledger 조회 및 txType 필터가 동작한다`() {
        val allResponse = get("/api/admin/finance/treasury-ledgers?page=0&size=50")
        assertThat(allResponse.statusCode).isEqualTo(HttpStatus.OK)
        val allItems = data(allResponse).path("items")
        assertThat(allItems.any { it.path("txType").asText() == "FIN_TEST_DEPOSIT" }).isTrue()
        assertThat(allItems.any { it.path("txType").asText() == "FIN_TEST_WITHDRAW" }).isTrue()

        val filteredResponse = get("/api/admin/finance/treasury-ledgers?txType=FIN_TEST_DEPOSIT&page=0&size=20")
        assertThat(filteredResponse.statusCode).isEqualTo(HttpStatus.OK)
        val filteredItems = data(filteredResponse).path("items")
        assertThat(filteredItems.isArray).isTrue()
        assertThat(filteredItems.size()).isGreaterThanOrEqualTo(1)
        assertThat(filteredItems.all { it.path("txType").asText() == "FIN_TEST_DEPOSIT" }).isTrue()
    }

    @Test
    fun `treasury ledger 조회는 sortDir asc desc를 지원한다`() {
        val desc = data(get("/api/admin/finance/treasury-ledgers?page=0&size=50&sortDir=desc")).path("items")
        val asc = data(get("/api/admin/finance/treasury-ledgers?page=0&size=50&sortDir=asc")).path("items")

        assertThat(desc.size()).isGreaterThanOrEqualTo(2)
        assertThat(asc.size()).isEqualTo(desc.size())
        val descFirst = desc.first().path("createdAt").asText()
        val descLast = desc.last().path("createdAt").asText()
        val ascFirst = asc.first().path("createdAt").asText()
        val ascLast = asc.last().path("createdAt").asText()
        assertThat(descFirst >= descLast).isTrue()
        assertThat(ascFirst <= ascLast).isTrue()
    }

    @Test
    fun `treasury ledger 조회는 sortBy amount를 지원한다`() {
        val asc = data(get("/api/admin/finance/treasury-ledgers?page=0&size=50&sortBy=amount&sortDir=asc")).path("items")
        val desc = data(get("/api/admin/finance/treasury-ledgers?page=0&size=50&sortBy=amount&sortDir=desc")).path("items")

        assertThat(asc.size()).isGreaterThanOrEqualTo(2)
        assertThat(desc.size()).isGreaterThanOrEqualTo(2)
        assertThat(asc.first().path("amount").decimalValue() <= asc.last().path("amount").decimalValue()).isTrue()
        assertThat(desc.first().path("amount").decimalValue() >= desc.last().path("amount").decimalValue()).isTrue()
    }

    private fun authHeaders() = HttpHeaders().apply {
        set(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
    }

    private fun get(path: String): ResponseEntity<JsonNode> =
        restTemplate.exchange(path, HttpMethod.GET, HttpEntity<Void>(authHeaders()), JsonNode::class.java)

    private fun data(response: ResponseEntity<JsonNode>): JsonNode = response.body!!.path("data")
}
