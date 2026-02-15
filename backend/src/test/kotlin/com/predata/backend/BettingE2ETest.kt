package com.predata.backend

import com.predata.backend.domain.*
import com.predata.backend.dto.CreateOrderRequest
import com.predata.backend.repository.*
import com.predata.backend.service.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * BET-019: E2E 시나리오 테스트
 *
 * 실제 사용자 흐름을 시뮬레이션하여 전체 베팅 시스템의 통합을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class BettingE2ETest {

    @Autowired
    private lateinit var orderMatchingService: OrderMatchingService

    @Autowired
    private lateinit var settlementService: SettlementService

    @Autowired
    private lateinit var positionService: PositionService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var tradeRepository: TradeRepository

    @Autowired
    private lateinit var positionRepository: PositionRepository

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    private lateinit var memberA: Member
    private lateinit var memberB: Member
    private lateinit var testQuestion: Question

    @BeforeEach
    fun setup() {
        // 테스트용 회원 A, B 생성
        memberA = memberRepository.save(
            Member(
                email = "memberA@example.com",
                countryCode = "KR",
                usdcBalance = BigDecimal("1000.00")
            )
        )

        memberB = memberRepository.save(
            Member(
                email = "memberB@example.com",
                countryCode = "KR",
                usdcBalance = BigDecimal("1000.00")
            )
        )

        // 테스트용 질문 생성
        testQuestion = questionRepository.save(
            Question(
                title = "Test Question for E2E",
                category = "TEST",
                status = QuestionStatus.BETTING,
                type = QuestionType.VERIFIABLE,
                votingEndAt = LocalDateTime.now().minusDays(1),
                bettingStartAt = LocalDateTime.now().minusHours(2),
                bettingEndAt = LocalDateTime.now().plusHours(2),
                expiredAt = LocalDateTime.now().plusDays(1)
            )
        )
    }

    @AfterEach
    fun cleanup() {
        tradeRepository.deleteAll()
        activityRepository.deleteAll()
        orderRepository.deleteAll()
        positionRepository.deleteAll()
        questionRepository.deleteAll()
        memberRepository.deleteAll()
    }

    /**
     * 시나리오 1: 시장가 주문 즉시 체결
     *
     * 흐름:
     * 1. 회원B가 NO를 0.40에 100수량 지정가 매수 주문 (오더북 등록)
     * 2. 회원A가 YES를 시장가로 50수량 매수 주문 (즉시 체결)
     * 3. 회원A의 Position 생성 확인
     * 4. Trade 기록 확인
     */
    @Test
    fun `시장가 시나리오 - 회원A가 YES 시장가 베팅하면 체결되고 Position 생성됨`() {
        // Given: 회원B가 NO 0.40 지정가 주문 (오더북에 등록)
        val orderBRequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.NO,
            price = BigDecimal("0.40"),
            amount = 100
        )
        val orderBResult = orderMatchingService.createOrder(memberB.id!!, orderBRequest)
        assertTrue(orderBResult.success, "회원B의 주문이 성공해야 합니다")

        // When: 회원A가 YES 시장가 주문 (역가격 0.60으로 매칭)
        val orderARequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.MARKET,
            side = OrderSide.YES,
            price = BigDecimal("0.60"), // 시장가는 이 값이 무시되고 자동 계산됨
            amount = 50
        )
        val orderAResult = orderMatchingService.createOrder(memberA.id!!, orderARequest)

        // Then: 주문 체결 확인
        assertTrue(orderAResult.success, "회원A의 시장가 주문이 성공해야 합니다")
        assertEquals(50, orderAResult.filledAmount, "50수량이 체결되어야 합니다")

        // Position 확인
        val positionA = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberA.id!!,
            testQuestion.id!!,
            OrderSide.YES
        )
        assertNotNull(positionA, "회원A의 YES 포지션이 생성되어야 합니다")
        assertEquals(BigDecimal("50.00"), positionA.quantity, "포지션 수량은 50이어야 합니다")

        val positionB = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberB.id!!,
            testQuestion.id!!,
            OrderSide.NO
        )
        assertNotNull(positionB, "회원B의 NO 포지션이 생성되어야 합니다")
        assertEquals(BigDecimal("50.00"), positionB.quantity, "포지션 수량은 50이어야 합니다")

        // Trade 기록 확인
        val trades = tradeRepository.findByQuestionIdOrderByExecutedAtDesc(testQuestion.id!!)
        assertEquals(1, trades.size, "1건의 Trade가 생성되어야 합니다")
        assertEquals(50L, trades[0].amount, "체결 수량은 50이어야 합니다")
    }

    /**
     * 시나리오 2: 지정가 주문 매칭
     *
     * 흐름:
     * 1. 회원A가 YES 0.60 지정가 주문
     * 2. 회원B가 NO 0.40 지정가 주문
     * 3. 가격이 매칭되어 체결
     * 4. 양쪽 Position 확인
     */
    @Test
    fun `지정가 시나리오 - YES 0-60과 NO 0-40이 매칭되어 체결되고 양쪽 Position 생성됨`() {
        // Given & When: 회원A가 YES 0.60 지정가 주문
        val orderARequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.YES,
            price = BigDecimal("0.60"),
            amount = 100
        )
        val orderAResult = orderMatchingService.createOrder(memberA.id!!, orderARequest)
        assertTrue(orderAResult.success, "회원A의 주문이 성공해야 합니다")

        // When: 회원B가 NO 0.40 지정가 주문 (매칭 발생)
        val orderBRequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.NO,
            price = BigDecimal("0.40"),
            amount = 100
        )
        val orderBResult = orderMatchingService.createOrder(memberB.id!!, orderBRequest)
        assertTrue(orderBResult.success, "회원B의 주문이 성공해야 합니다")

        // Then: 체결 확인
        assertEquals(100, orderBResult.filledAmount, "100수량이 체결되어야 합니다")

        // Position 확인
        val positionA = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberA.id!!,
            testQuestion.id!!,
            OrderSide.YES
        )
        assertNotNull(positionA, "회원A의 YES 포지션이 생성되어야 합니다")
        assertEquals(BigDecimal("100.00"), positionA.quantity, "회원A 포지션 수량은 100이어야 합니다")

        val positionB = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberB.id!!,
            testQuestion.id!!,
            OrderSide.NO
        )
        assertNotNull(positionB, "회원B의 NO 포지션이 생성되어야 합니다")
        assertEquals(BigDecimal("100.00"), positionB.quantity, "회원B 포지션 수량은 100이어야 합니다")

        // Trade 확인
        val trades = tradeRepository.findByQuestionIdOrderByExecutedAtDesc(testQuestion.id!!)
        assertEquals(1, trades.size, "1건의 Trade가 생성되어야 합니다")
        assertEquals(100L, trades[0].amount, "체결 수량은 100이어야 합니다")
    }

    /**
     * 시나리오 3: 부분 체결
     *
     * 흐름:
     * 1. 회원A가 YES 100수량 주문
     * 2. 회원B가 NO 50수량 주문
     * 3. 50수량만 체결, 회원A의 주문 50수량 미체결 잔량 확인
     */
    @Test
    fun `부분체결 시나리오 - 회원A YES 100수량 주문에 회원B NO 50수량 주문시 50만 체결되고 50은 미체결`() {
        // Given: 회원A가 YES 100수량 지정가 주문
        val orderARequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.YES,
            price = BigDecimal("0.60"),
            amount = 100
        )
        val orderAResult = orderMatchingService.createOrder(memberA.id!!, orderARequest)
        assertTrue(orderAResult.success, "회원A의 주문이 성공해야 합니다")
        assertEquals(0, orderAResult.filledAmount, "초기에는 체결되지 않아야 합니다")

        // When: 회원B가 NO 50수량 지정가 주문 (부분 체결)
        val orderBRequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.NO,
            price = BigDecimal("0.40"),
            amount = 50
        )
        val orderBResult = orderMatchingService.createOrder(memberB.id!!, orderBRequest)
        assertTrue(orderBResult.success, "회원B의 주문이 성공해야 합니다")
        assertEquals(50, orderBResult.filledAmount, "50수량이 체결되어야 합니다")

        // Then: 회원A의 주문 상태 확인
        val orderA = orderRepository.findById(orderAResult.orderId!!).orElseThrow()
        assertEquals(OrderStatus.PARTIAL, orderA.status, "회원A의 주문은 PARTIAL 상태여야 합니다")
        assertEquals(50L, orderA.remainingAmount, "미체결 잔량은 50이어야 합니다")

        // 회원B의 주문 상태 확인
        val orderB = orderRepository.findById(orderBResult.orderId!!).orElseThrow()
        assertEquals(OrderStatus.FILLED, orderB.status, "회원B의 주문은 FILLED 상태여야 합니다")
        assertEquals(0L, orderB.remainingAmount, "미체결 잔량은 0이어야 합니다")

        // Position 확인
        val positionA = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberA.id!!,
            testQuestion.id!!,
            OrderSide.YES
        )
        assertNotNull(positionA, "회원A의 포지션이 생성되어야 합니다")
        assertEquals(BigDecimal("50.00"), positionA.quantity, "회원A 포지션 수량은 50이어야 합니다")
    }

    /**
     * 시나리오 4: 만기 차단
     *
     * 흐름:
     * 1. expiredAt이 과거인 질문 생성
     * 2. 주문 시도
     * 3. 거부 확인
     */
    @Test
    fun `만기차단 시나리오 - expiredAt 지난 question에 주문 시도시 거부됨`() {
        // Given: 만료된 질문 생성
        val expiredQuestion = questionRepository.save(
            Question(
                title = "Expired Question",
                category = "TEST",
                status = QuestionStatus.BETTING,
                type = QuestionType.VERIFIABLE,
                votingEndAt = LocalDateTime.now().minusDays(2),
                bettingStartAt = LocalDateTime.now().minusDays(1),
                bettingEndAt = LocalDateTime.now().minusHours(1),
                expiredAt = LocalDateTime.now().minusHours(1) // 이미 만료됨
            )
        )

        // When: 주문 시도
        val orderRequest = CreateOrderRequest(
            questionId = expiredQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.YES,
            price = BigDecimal("0.60"),
            amount = 100
        )
        val result = orderMatchingService.createOrder(memberA.id!!, orderRequest)

        // Then: 거부 확인
        assertFalse(result.success, "만료된 질문에 대한 주문은 거부되어야 합니다")
        val message = result.message ?: ""
        assertTrue(
            message.contains("만료") || message.contains("기간"),
            "에러 메시지에 '만료' 또는 '기간'이 포함되어야 합니다"
        )

        // 주문이 생성되지 않았는지 확인
        val orders = orderRepository.findByQuestionIdAndStatusIn(
            expiredQuestion.id!!,
            listOf(OrderStatus.OPEN, OrderStatus.PARTIAL, OrderStatus.FILLED)
        )
        assertTrue(orders.isEmpty(), "주문이 생성되지 않아야 합니다")
    }

    /**
     * 시나리오 5: 정산 흐름
     *
     * 흐름:
     * 1. 회원A YES 포지션, 회원B NO 포지션 생성
     * 2. 정산 (최종 결과: YES)
     * 3. 회원A는 지급, 회원B는 미지급
     * 4. settled=true 확인
     */
    @Test
    fun `정산 시나리오 - Position 있는 상태에서 정산시 winning side 지급 및 settled=true`() {
        // Given: 포지션 생성 (매칭을 통해)
        // 회원A: YES 100
        val orderARequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.YES,
            price = BigDecimal("0.60"),
            amount = 100
        )
        orderMatchingService.createOrder(memberA.id!!, orderARequest)

        // 회원B: NO 100
        val orderBRequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.LIMIT,
            side = OrderSide.NO,
            price = BigDecimal("0.40"),
            amount = 100
        )
        orderMatchingService.createOrder(memberB.id!!, orderBRequest)

        val initialBalanceA = memberRepository.findById(memberA.id!!).orElseThrow().usdcBalance
        val initialBalanceB = memberRepository.findById(memberB.id!!).orElseThrow().usdcBalance

        // When: 정산 (최종 결과: YES)
        settlementService.initiateSettlement(
            questionId = testQuestion.id!!,
            finalResult = FinalResult.YES,
            sourceUrl = "http://test.com"
        )

        // 정산 확정 (이의제기 기간 스킵)
        val finalizeResult = settlementService.finalizeSettlement(
            questionId = testQuestion.id!!,
            skipDeadlineCheck = true
        )

        // Then: 정산 결과 확인
        assertTrue(finalizeResult.totalWinners > 0, "승자가 있어야 합니다")
        assertTrue(finalizeResult.totalPayout > 0, "지급액이 있어야 합니다")

        // 회원A (YES 승리) 잔액 증가 확인
        val finalBalanceA = memberRepository.findById(memberA.id!!).orElseThrow().usdcBalance
        assertTrue(finalBalanceA > initialBalanceA, "회원A의 잔액이 증가해야 합니다")

        // 회원B (NO 패배) 잔액 변화 없음
        val finalBalanceB = memberRepository.findById(memberB.id!!).orElseThrow().usdcBalance
        assertEquals(initialBalanceB, finalBalanceB, "회원B의 잔액은 변화 없어야 합니다 (패배)")

        // Position settled=true 확인
        val positionA = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberA.id!!,
            testQuestion.id!!,
            OrderSide.YES
        )
        assertNotNull(positionA, "회원A의 포지션이 존재해야 합니다")
        assertTrue(positionA.settled, "포지션은 settled=true여야 합니다")

        val positionB = positionRepository.findByMemberIdAndQuestionIdAndSide(
            memberB.id!!,
            testQuestion.id!!,
            OrderSide.NO
        )
        assertNotNull(positionB, "회원B의 포지션이 존재해야 합니다")
        assertTrue(positionB.settled, "포지션은 settled=true여야 합니다")

        // 질문 상태 확인
        val settledQuestion = questionRepository.findById(testQuestion.id!!).orElseThrow()
        assertEquals(QuestionStatus.SETTLED, settledQuestion.status, "질문은 SETTLED 상태여야 합니다")
        assertEquals(FinalResult.YES, settledQuestion.finalResult, "최종 결과는 YES여야 합니다")
    }

    /**
     * 시나리오 6: IOC (Immediate Or Cancel) 주문
     *
     * 흐름:
     * 1. 오더북이 비어있는 상태
     * 2. 시장가 주문 생성
     * 3. 체결되지 않으면 자동 취소
     * 4. 잔액 환불 확인
     */
    @Test
    fun `IOC 시나리오 - 호가 없는 상태에서 MARKET 주문시 미체결분 자동 취소되고 환불됨`() {
        // Given: 빈 오더북 (호가 없음)
        val initialBalance = memberRepository.findById(memberA.id!!).orElseThrow().usdcBalance

        // When: 시장가 주문 생성 (호가 없어서 체결 불가)
        val orderRequest = CreateOrderRequest(
            questionId = testQuestion.id!!,
            orderType = OrderType.MARKET,
            side = OrderSide.YES,
            price = BigDecimal("0.60"), // 시장가는 이 값이 무시됨
            amount = 100
        )
        val result = orderMatchingService.createOrder(memberA.id!!, orderRequest)

        // Then: 주문 실패 확인 (호가 없음)
        assertFalse(result.success, "호가가 없을 때 시장가 주문은 실패해야 합니다")
        val message = result.message ?: ""
        assertTrue(
            message.contains("호가") || message.contains("실패"),
            "에러 메시지에 '호가' 또는 '실패'가 포함되어야 합니다"
        )

        // 잔액이 원상복구되었는지 확인
        val finalBalance = memberRepository.findById(memberA.id!!).orElseThrow().usdcBalance
        assertEquals(initialBalance, finalBalance, "잔액이 원상복구되어야 합니다")

        // 주문이 생성되지 않았거나 CANCELLED 상태인지 확인
        val orders = orderRepository.findByMemberIdAndQuestionId(memberA.id!!, testQuestion.id!!)
        assertTrue(
            orders.isEmpty() || orders.all { it.status == OrderStatus.CANCELLED },
            "주문이 생성되지 않았거나 CANCELLED 상태여야 합니다"
        )
    }
}
