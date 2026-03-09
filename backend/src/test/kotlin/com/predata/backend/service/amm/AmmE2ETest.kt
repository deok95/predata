package com.predata.backend.service.amm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.config.JwtUtil
import com.predata.backend.domain.ExecutionModel
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Member
import com.predata.backend.domain.PoolStatus
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import com.predata.backend.dto.amm.SeedPoolRequest
import com.predata.backend.dto.amm.SeedPoolResponse
import com.predata.backend.dto.amm.SwapRequest
import com.predata.backend.dto.amm.SwapResponse
import com.predata.backend.dto.amm.SwapSimulationResponse
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.FeePoolLedgerRepository
import com.predata.backend.repository.FeePoolRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.TransactionHistoryRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.repository.amm.SwapHistoryRepository
import com.predata.backend.repository.amm.UserSharesRepository
import com.predata.backend.service.SettlementService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class AmmE2ETest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var marketPoolRepository: MarketPoolRepository

    @Autowired
    private lateinit var userSharesRepository: UserSharesRepository

    @Autowired
    private lateinit var swapHistoryRepository: SwapHistoryRepository

    @Autowired
    private lateinit var feePoolRepository: FeePoolRepository

    @Autowired
    private lateinit var feePoolLedgerRepository: FeePoolLedgerRepository

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    @Autowired
    private lateinit var transactionHistoryRepository: TransactionHistoryRepository

    @Autowired
    private lateinit var settlementService: SettlementService

    private lateinit var admin: Member
    private lateinit var userA: Member
    private lateinit var userB: Member
    private lateinit var question: Question

    private lateinit var adminToken: String
    private lateinit var userAToken: String
    private lateinit var userBToken: String

    // Saved between ordered tests
    private var noPriceAfterUserABuy: BigDecimal? = null
    private var userAYesSharesAfterBuy: BigDecimal? = null

    companion object {
        private val SCALE = 18
        private val ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
        private val ONE = BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)

        private fun bd(value: String): BigDecimal = BigDecimal(value).setScale(SCALE, RoundingMode.HALF_UP)
    }

    @BeforeAll
    fun setup() {
        admin = memberRepository.save(
            Member(
                email = "amm-admin@example.com",
                countryCode = "KR",
                role = "ADMIN",
                usdcBalance = BigDecimal.ZERO
            )
        )
        userA = memberRepository.save(
            Member(
                email = "amm-userA@example.com",
                countryCode = "KR",
                role = "USER",
                usdcBalance = BigDecimal("1000.00")
            )
        )
        userB = memberRepository.save(
            Member(
                email = "amm-userB@example.com",
                countryCode = "KR",
                role = "USER",
                usdcBalance = BigDecimal("1000.00")
            )
        )

        question = questionRepository.save(
            Question(
                title = "AMM E2E Test Question",
                category = "TEST",
                status = QuestionStatus.BETTING,
                type = QuestionType.VERIFIABLE,
                votingEndAt = LocalDateTime.now().minusDays(1),
                bettingStartAt = LocalDateTime.now().minusHours(2),
                bettingEndAt = LocalDateTime.now().plusHours(2),
                expiredAt = LocalDateTime.now().plusDays(1),
                executionModel = ExecutionModel.AMM_FPMM
            )
        )

        adminToken = jwtUtil.generateToken(admin.id!!, admin.email, "ADMIN")
        userAToken = jwtUtil.generateToken(userA.id!!, userA.email, "USER")
        userBToken = jwtUtil.generateToken(userB.id!!, userB.email, "USER")
    }

    @AfterAll
    fun cleanup() {
        val questionId = question.id ?: return

        runCatching {
            // swap_history
            val swaps = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
                questionId,
                org.springframework.data.domain.PageRequest.of(0, 500)
            ).content
            swapHistoryRepository.deleteAll(swaps)
        }

        runCatching {
            // user_shares
            val shares = userSharesRepository.findByQuestionId(questionId)
            userSharesRepository.deleteAll(shares)
        }

        runCatching {
            // market_pools
            marketPoolRepository.findById(questionId).ifPresent { marketPoolRepository.delete(it) }
        }

        runCatching {
            // fee pools + ledgers
            val feePool = feePoolRepository.findByQuestionId(questionId).orElse(null)
            if (feePool != null) {
                val ledgers = feePoolLedgerRepository.findByFeePoolIdOrderByCreatedAtDesc(feePool.id!!)
                feePoolLedgerRepository.deleteAll(ledgers)
                feePoolRepository.delete(feePool)
            }
        }

        runCatching {
            // activity + tx history (filter by questionId)
            activityRepository.deleteAll(activityRepository.findByQuestionId(questionId))
            val txs = transactionHistoryRepository.findAll().filter { it.questionId == questionId }
            transactionHistoryRepository.deleteAll(txs)
        }

        runCatching { questionRepository.deleteById(questionId) }
        runCatching { memberRepository.deleteById(admin.id!!) }
        runCatching { memberRepository.deleteById(userA.id!!) }
        runCatching { memberRepository.deleteById(userB.id!!) }
    }

    @Test
    @Order(1)
    fun `Test 1 - seedPool`() {
        val questionId = question.id!!

        val request = SeedPoolRequest(
            questionId = questionId,
            seedUsdc = bd("1000"),
            feeRate = bd("0.01")
        )

        val response = postJson(
            path = "/api/pool/seed",
            token = adminToken,
            body = request
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val data = parseSuccessData(response.body!!, SeedPoolResponse::class.java)

        assertBigDecimalClose(bd("1000"), data.yesShares, bd("0.000001"))
        assertBigDecimalClose(bd("1000"), data.noShares, bd("0.000001"))
        assertBigDecimalClose(bd("1000"), data.collateralLocked, bd("0.000001"))
        assertBigDecimalClose(bd("0.5"), data.currentPrice.yes, bd("0.000001"))
        assertBigDecimalClose(bd("0.5"), data.currentPrice.no, bd("0.000001"))

        // DB
        val pool = marketPoolRepository.findById(questionId).orElseThrow()
        assertEquals(PoolStatus.ACTIVE, pool.status)
        assertBigDecimalClose(bd("1000"), pool.yesShares, bd("0.000001"))
        assertBigDecimalClose(bd("1000"), pool.noShares, bd("0.000001"))
        assertBigDecimalClose(bd("1000"), pool.collateralLocked, bd("0.000001"))
    }

    @Test
    @Order(2)
    fun `Test 2 - getPoolState without auth`() {
        val questionId = question.id!!
        val response = restTemplate.getForEntity("/api/pool/$questionId", String::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)

        val data = parseSuccessData(response.body!!, com.predata.backend.dto.amm.PoolStateResponse::class.java)
        assertBigDecimalClose(bd("0.5"), data.currentPrice.yes, bd("0.000001"))
        assertBigDecimalClose(bd("0.5"), data.currentPrice.no, bd("0.000001"))
    }

    @Test
    @Order(3)
    fun `Test 3 - simulateSwap without auth`() {
        val questionId = question.id!!
        val uri = UriComponentsBuilder
            .fromPath("/api/swap/simulate")
            .queryParam("questionId", questionId)
            .queryParam("action", "BUY")
            .queryParam("outcome", "YES")
            .queryParam("amount", "100")
            .build()
            .toUri()

        val response = restTemplate.getForEntity(uri, String::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)

        val data = parseSuccessData(response.body!!, SwapSimulationResponse::class.java)
        assertNotNull(data.sharesOut)
        assertTrue(data.sharesOut!! > ZERO)
        assertTrue(data.fee > ZERO)
        assertTrue(data.slippage >= ZERO)
    }

    @Test
    @Order(4)
    fun `Test 4 - userA BUY YES 100 USDC`() {
        val questionId = question.id!!
        val request = SwapRequest(
            questionId = questionId,
            action = SwapAction.BUY,
            outcome = ShareOutcome.YES,
            usdcIn = bd("100")
        )

        val response = postJson(
            path = "/api/swap",
            token = userAToken,
            body = request
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val data = parseSuccessData(response.body!!, SwapResponse::class.java)

        assertTrue(data.sharesAmount > bd("180") && data.sharesAmount < bd("200"))
        assertTrue(data.effectivePrice > bd("0.5"))
        assertBigDecimalClose(bd("1.0"), data.fee, bd("0.000001"))
        assertTrue(data.priceAfter.yes > bd("0.5"))
        assertTrue(data.priceAfter.no < bd("0.5"))
        assertTrue(data.myShares.yesShares > ZERO)

        noPriceAfterUserABuy = data.priceAfter.no

        // DB
        val memberA = memberRepository.findById(userA.id!!).orElseThrow()
        assertBigDecimalClose(BigDecimal("900.00"), memberA.usdcBalance, bd("0.000001"))

        val userShares = userSharesRepository.findById(
            com.predata.backend.domain.UserSharesId(userA.id!!, questionId, ShareOutcome.YES)
        ).orElseThrow()
        userAYesSharesAfterBuy = userShares.shares
        assertTrue(userShares.shares > bd("180") && userShares.shares < bd("200"))
        // Expected ~189.08
        assertBigDecimalClose(bd("189.08189262966333"), userShares.shares, bd("0.05"))

        val pool = marketPoolRepository.findById(questionId).orElseThrow()
        assertBigDecimalClose(bd("1099"), pool.collateralLocked, bd("0.000001"))

        val buyCount = swapHistoryRepository.countByQuestionIdAndAction(questionId, SwapAction.BUY)
        assertEquals(1L, buyCount)
    }

    @Test
    @Order(5)
    fun `Test 5 - userB BUY NO 50 USDC`() {
        val questionId = question.id!!
        val request = SwapRequest(
            questionId = questionId,
            action = SwapAction.BUY,
            outcome = ShareOutcome.NO,
            usdcIn = bd("50")
        )

        val response = postJson(
            path = "/api/swap",
            token = userBToken,
            body = request
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val data = parseSuccessData(response.body!!, SwapResponse::class.java)

        assertTrue(data.sharesAmount > ZERO)

        val prevNoPrice = noPriceAfterUserABuy ?: error("prev no price not set")
        assertTrue(data.priceAfter.no > prevNoPrice, "NO price should increase after buying NO")

        // DB
        val memberB = memberRepository.findById(userB.id!!).orElseThrow()
        assertBigDecimalClose(BigDecimal("950.00"), memberB.usdcBalance, bd("0.000001"))

        val userShares = userSharesRepository.findById(
            com.predata.backend.domain.UserSharesId(userB.id!!, questionId, ShareOutcome.NO)
        ).orElseThrow()
        assertTrue(userShares.shares > ZERO)

        val buyCount = swapHistoryRepository.countByQuestionIdAndAction(questionId, SwapAction.BUY)
        assertEquals(2L, buyCount)
    }

    @Test
    @Order(6)
    fun `Test 6 - userA SELL YES 50 shares`() {
        val questionId = question.id!!
        val beforeShares = userAYesSharesAfterBuy ?: error("userA shares after buy not set")
        val beforeCostBasis = userSharesRepository.findById(
            com.predata.backend.domain.UserSharesId(userA.id!!, questionId, ShareOutcome.YES)
        ).orElseThrow().costBasisUsdc

        val request = SwapRequest(
            questionId = questionId,
            action = SwapAction.SELL,
            outcome = ShareOutcome.YES,
            sharesIn = bd("50")
        )

        val response = postJson(
            path = "/api/swap",
            token = userAToken,
            body = request
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val data = parseSuccessData(response.body!!, SwapResponse::class.java)

        assertTrue(data.usdcAmount > ZERO)
        assertTrue(data.fee > ZERO)

        // DB
        val memberA = memberRepository.findById(userA.id!!).orElseThrow()
        assertTrue(memberA.usdcBalance > BigDecimal("900.00"))

        val userShares = userSharesRepository.findById(
            com.predata.backend.domain.UserSharesId(userA.id!!, questionId, ShareOutcome.YES)
        ).orElseThrow()
        val expectedRemainingShares = beforeShares.subtract(bd("50"))
        assertBigDecimalClose(expectedRemainingShares, userShares.shares, bd("0.001"))

        // costBasis 비례 차감: remaining = oldCostBasis * (remainingShares / oldShares)
        val expectedCostBasis = beforeCostBasis
            .multiply(userShares.shares)
            .divide(beforeShares, SCALE, RoundingMode.HALF_UP)
        assertBigDecimalClose(expectedCostBasis, userShares.costBasisUsdc, bd("0.001"))
    }

    @Test
    @Order(7)
    fun `Test 7 - slippage protection`() {
        val questionId = question.id!!
        val beforeCount = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
            questionId,
            org.springframework.data.domain.PageRequest.of(0, 500)
        ).totalElements

        val request = SwapRequest(
            questionId = questionId,
            action = SwapAction.BUY,
            outcome = ShareOutcome.YES,
            usdcIn = bd("100"),
            minSharesOut = bd("999999")
        )

        val response = postJson(
            path = "/api/swap",
            token = userAToken,
            body = request
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val afterCount = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
            questionId,
            org.springframework.data.domain.PageRequest.of(0, 500)
        ).totalElements
        assertEquals(beforeCount, afterCount, "DB should not change on slippage rejection")
    }

    @Test
    @Order(8)
    fun `Test 8 - insufficient balance`() {
        val questionId = question.id!!
        val beforeCount = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
            questionId,
            org.springframework.data.domain.PageRequest.of(0, 500)
        ).totalElements

        val request = SwapRequest(
            questionId = questionId,
            action = SwapAction.BUY,
            outcome = ShareOutcome.YES,
            usdcIn = bd("999999")
        )

        val response = postJson(
            path = "/api/swap",
            token = userAToken,
            body = request
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val afterCount = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
            questionId,
            org.springframework.data.domain.PageRequest.of(0, 500)
        ).totalElements
        assertEquals(beforeCount, afterCount, "DB should not change on balance rejection")
    }

    @Test
    @Order(9)
    fun `Test 9 - over SELL rejection`() {
        val questionId = question.id!!
        val beforeCount = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
            questionId,
            org.springframework.data.domain.PageRequest.of(0, 500)
        ).totalElements

        val request = SwapRequest(
            questionId = questionId,
            action = SwapAction.SELL,
            outcome = ShareOutcome.YES,
            sharesIn = bd("999999")
        )

        val response = postJson(
            path = "/api/swap",
            token = userAToken,
            body = request
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val afterCount = swapHistoryRepository.findByQuestionIdOrderByCreatedAtDesc(
            questionId,
            org.springframework.data.domain.PageRequest.of(0, 500)
        ).totalElements
        assertEquals(beforeCount, afterCount, "DB should not change on over-sell rejection")
    }

    @Test
    @Order(10)
    fun `Test 10 - settlement YES wins`() {
        val questionId = question.id!!

        val memberABefore = memberRepository.findById(userA.id!!).orElseThrow()
        val memberBBefore = memberRepository.findById(userB.id!!).orElseThrow()

        val poolBefore = marketPoolRepository.findById(questionId).orElseThrow()

        val remainingYesShares = userSharesRepository.findById(
            com.predata.backend.domain.UserSharesId(userA.id!!, questionId, ShareOutcome.YES)
        ).orElseThrow().shares

        settlementService.initiateSettlement(questionId, FinalResult.YES, "TEST")
        settlementService.finalizeSettlement(questionId, skipDeadlineCheck = true)

        val memberAAfter = memberRepository.findById(userA.id!!).orElseThrow()
        val memberBAfter = memberRepository.findById(userB.id!!).orElseThrow()

        // userA paid out remaining YES shares
        val expectedA = memberABefore.usdcBalance.add(remainingYesShares).setScale(6, RoundingMode.HALF_UP)
        assertBigDecimalClose(expectedA, memberAAfter.usdcBalance.setScale(6, RoundingMode.HALF_UP), bd("0.001"))

        // userB unchanged (lost)
        assertBigDecimalClose(memberBBefore.usdcBalance, memberBAfter.usdcBalance, bd("0.000001"))

        // user_shares reset
        val allShares = userSharesRepository.findByQuestionId(questionId)
        allShares.forEach {
            assertBigDecimalClose(ZERO, it.shares, bd("0.000001"))
            assertBigDecimalClose(ZERO, it.costBasisUsdc, bd("0.000001"))
        }

        // pool settled + collateralLocked reduced by payout
        val poolAfter = marketPoolRepository.findById(questionId).orElseThrow()
        assertEquals(PoolStatus.SETTLED, poolAfter.status)
        assertTrue(poolAfter.collateralLocked >= ZERO)
        val expectedLocked = poolBefore.collateralLocked.subtract(remainingYesShares).setScale(18, RoundingMode.DOWN)
        assertBigDecimalClose(expectedLocked, poolAfter.collateralLocked, bd("0.001"))

        // question status + marker
        val qAfter = questionRepository.findById(questionId).orElseThrow()
        assertEquals(QuestionStatus.SETTLED, qAfter.status)
        assertEquals(null, qAfter.disputeDeadline)
    }

    @Test
    @Order(11)
    fun `Test 11 - double settlement prevented`() {
        val questionId = question.id!!
        val ex = Assertions.assertThrows(IllegalStateException::class.java) {
            settlementService.finalizeSettlement(questionId, skipDeadlineCheck = true)
        }
        assertTrue(
            (ex.message ?: "").contains("already finalized", ignoreCase = true),
            "Error message should indicate settlement was already finalized"
        )
    }

    @Test
    @Order(12)
    fun `Test 12 - auth required endpoints`() {
        val questionId = question.id!!

        // POST /api/swap without token -> 401
        val swapReq = SwapRequest(
            questionId = questionId,
            action = SwapAction.BUY,
            outcome = ShareOutcome.YES,
            usdcIn = bd("1")
        )
        val swapResponse = postJson(
            path = "/api/swap",
            token = null,
            body = swapReq
        )
        assertEquals(HttpStatus.UNAUTHORIZED, swapResponse.statusCode)

        // POST /api/pool/seed with user token -> 403
        val seedReq = SeedPoolRequest(
            questionId = questionId,
            seedUsdc = bd("1000"),
            feeRate = bd("0.01")
        )
        val seedResponse = postJson(
            path = "/api/pool/seed",
            token = userAToken,
            body = seedReq
        )
        assertEquals(HttpStatus.FORBIDDEN, seedResponse.statusCode)
    }

    @Test
    @Order(13)
    fun `Final - conservation check`() {
        val questionId = question.id!!

        val memberA = memberRepository.findById(userA.id!!).orElseThrow()
        val memberB = memberRepository.findById(userB.id!!).orElseThrow()
        val pool = marketPoolRepository.findById(questionId).orElseThrow()
        val feePool = feePoolRepository.findByQuestionId(questionId).orElseThrow()

        val initialTotal = bd("3000")
        val finalTotal = memberA.usdcBalance
            .add(memberB.usdcBalance)
            .add(pool.collateralLocked)
            .add(feePool.totalFees)
            .setScale(2, RoundingMode.HALF_UP)

        val diff = finalTotal.subtract(initialTotal).abs()
        assertTrue(diff <= bd("0.01"), "Conservation failed: initial=$initialTotal, final=$finalTotal, diff=$diff")
    }

    private fun postJson(path: String, token: String?, body: Any): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        if (token != null) {
            headers.setBearerAuth(token)
        }
        return restTemplate.exchange(path, HttpMethod.POST, HttpEntity(body, headers), String::class.java)
    }

    private fun <T> parseSuccessData(json: String, clazz: Class<T>): T {
        val root = objectMapper.readTree(json)
        val success = root.path("success").asBoolean(false)
        assertTrue(success, "expected success=true but got: $json")
        val dataNode = root.path("data")
        assertTrue(!dataNode.isMissingNode && !dataNode.isNull, "missing data in response: $json")
        return objectMapper.treeToValue(dataNode, clazz)
    }

    private fun assertBigDecimalClose(expected: BigDecimal, actual: BigDecimal, tolerance: BigDecimal) {
        val e = expected.setScale(expected.scale().coerceAtLeast(6), RoundingMode.HALF_UP)
        val a = actual.setScale(e.scale(), RoundingMode.HALF_UP)
        val diff = e.subtract(a).abs()
        assertTrue(diff <= tolerance, "expected=$expected actual=$actual diff=$diff tol=$tolerance")
    }
}
