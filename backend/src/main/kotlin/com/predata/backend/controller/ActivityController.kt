package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.domain.ActivityType
import com.predata.backend.service.VoteService
import com.predata.backend.service.BetService
import com.predata.backend.service.QuestionService
import com.predata.backend.service.SettlementService
import com.predata.backend.service.OddsService
import com.predata.backend.service.ClientIpService
import com.predata.backend.domain.FinalResult
import com.predata.backend.repository.ActivityRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
class ActivityController(
    private val voteService: VoteService,
    private val betService: BetService,
    private val betSellService: com.predata.backend.service.BetSellService,
    private val questionService: QuestionService,
    private val settlementService: SettlementService,
    private val oddsService: OddsService,
    private val clientIpService: ClientIpService,
    private val bettingSuspensionService: com.predata.backend.service.BettingSuspensionService,
    private val activityRepository: ActivityRepository,
    private val orderMatchingService: com.predata.backend.service.OrderMatchingService
) {

    /**
     * 투표 실행
     */
    @PostMapping("/vote")
    fun vote(@Valid @RequestBody request: VoteRequest, httpRequest: HttpServletRequest): ResponseEntity<ActivityResponse> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ActivityResponse(success = false, message = "Authentication required.")
            )

        val clientIp = clientIpService.extractClientIp(httpRequest)
        val response = voteService.vote(authenticatedMemberId, request, clientIp)

        // IP 업데이트
        if (response.success) clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    /**
     * 베팅 실행 (시장가 IOC 주문으로 전환)
     * 기존 BetService 대신 OrderMatchingService 사용
     */
    @PostMapping("/bet")
    fun bet(@Valid @RequestBody request: BetRequest, httpRequest: HttpServletRequest): ResponseEntity<ActivityResponse> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ActivityResponse(success = false, message = "Authentication required.")
            )

        val clientIp = clientIpService.extractClientIp(httpRequest)

        // BetRequest를 CreateOrderRequest로 변환 (시장가 IOC 주문)
        val orderRequest = CreateOrderRequest(
            questionId = request.questionId,
            side = when (request.choice) {
                com.predata.backend.domain.Choice.YES -> com.predata.backend.domain.OrderSide.YES
                com.predata.backend.domain.Choice.NO -> com.predata.backend.domain.OrderSide.NO
            },
            price = java.math.BigDecimal.ZERO,  // MARKET 주문은 가격 무시됨 (OrderMatchingService에서 호가로 결정)
            amount = request.amount,
            orderType = com.predata.backend.domain.OrderType.MARKET  // 시장가 IOC
        )

        // OrderMatchingService로 주문 실행
        val orderResponse = orderMatchingService.createOrder(authenticatedMemberId, orderRequest)

        // IP 업데이트
        if (orderResponse.success) clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)

        // ActivityResponse로 변환
        val activityResponse = ActivityResponse(
            success = orderResponse.success,
            message = orderResponse.message,
            activityId = orderResponse.orderId
        )

        return if (orderResponse.success) {
            ResponseEntity.ok(activityResponse)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(activityResponse)
        }
    }

    /**
     * 베팅 판매 (포지션 청산)
     * POST /api/bets/{betId}/sell
     */
    @PostMapping("/bets/{betId}/sell")
    fun sellBet(
        @PathVariable betId: Long,
        @Valid @RequestBody request: SellBetRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SellBetResponse> {
        // AMM 판매 기능 비활성화 - 410 Gone 반환
        return ResponseEntity.status(410).body(
            SellBetResponse(
                success = false,
                message = "AMM selling is no longer supported. You can close your position using counter orders in the Market tab."
            )
        )
    }

    /**
     * 모든 질문 조회
     */
    @GetMapping("/questions")
    fun getAllQuestions(): ResponseEntity<ApiEnvelope<List<QuestionResponse>>> {
        val questions = questionService.getAllQuestions()
        return ResponseEntity.ok(ApiEnvelope(success = true, data = questions))
    }

    /**
     * 특정 질문 조회
     */
    @GetMapping("/questions/{id}")
    fun getQuestion(@PathVariable id: Long): ResponseEntity<ApiEnvelope<QuestionResponse>> {
        val question = questionService.getQuestion(id)

        return if (question != null) {
            ResponseEntity.ok(ApiEnvelope(success = true, data = question))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiEnvelope(success = false, data = null, message = "Question not found.")
            )
        }
    }

    /**
     * 질문 조회수 증가
     */
    @PostMapping("/questions/{id}/view")
    fun incrementViewCount(@PathVariable id: Long): ResponseEntity<ApiEnvelope<Boolean>> {
        val success = questionService.incrementViewCount(id)
        return if (success) {
            ResponseEntity.ok(ApiEnvelope(success = true, data = true))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiEnvelope(success = false, data = null, message = "Question not found.")
            )
        }
    }

    /**
     * 상태별 질문 조회
     */
    @GetMapping("/questions/status/{status}")
    fun getQuestionsByStatus(@PathVariable status: String): ResponseEntity<ApiEnvelope<List<QuestionResponse>>> {
        val questionStatus = com.predata.backend.domain.QuestionStatus.valueOf(status.uppercase())
        val questions = questionService.getQuestionsByStatus(questionStatus)
        return ResponseEntity.ok(ApiEnvelope(success = true, data = questions))
    }

    /**
     * 본인의 투표/베팅 내역 조회 (JWT 인증 사용)
     * GET /api/activities/me?type=VOTE
     */
    @GetMapping("/activities/me")
    fun getMyActivities(
        @RequestParam(required = false) type: String?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<List<MemberActivityView>> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByMemberIdAndActivityType(authenticatedMemberId, activityType)
        } else {
            activityRepository.findByMemberId(authenticatedMemberId)
        }

        val result = activities.map(ActivityViewAssembler::toMemberActivityView)

        return ResponseEntity.ok(result)
    }

    /**
     * 특정 질문의 모든 활동 조회
     * GET /api/activities/question/{questionId}
     */
    @GetMapping("/activities/question/{questionId}")
    fun getActivitiesByQuestion(
        @PathVariable questionId: Long,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<QuestionActivityView>> {
        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByQuestionIdAndActivityType(questionId, activityType)
        } else {
            activityRepository.findByQuestionId(questionId)
        }

        val result = activities.map(ActivityViewAssembler::toQuestionActivityView)

        return ResponseEntity.ok(result)
    }

    /**
     * 본인의 특정 질문에 대한 활동 조회 (JWT 인증 사용)
     * GET /api/activities/me/question/{questionId}
     */
    @GetMapping("/activities/me/question/{questionId}")
    fun getMyActivitiesByQuestion(
        @PathVariable questionId: Long,
        @RequestParam(required = false) type: String?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<List<MemberQuestionActivityView>> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        val activities = activityRepository.findByMemberIdAndQuestionId(authenticatedMemberId, questionId)

        val filtered = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activities.filter { it.activityType == activityType }
        } else {
            activities
        }

        val result = filtered.map(ActivityViewAssembler::toMemberQuestionActivityView)

        return ResponseEntity.ok(result)
    }

    /**
     * 배당률 계산 (실시간)
     * GET /api/questions/{id}/odds
     */
    @GetMapping("/questions/{id}/odds")
    fun getOdds(@PathVariable id: Long): ResponseEntity<ApiEnvelope<OddsResponse>> {
        val question = questionService.getQuestion(id)

        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiEnvelope(success = false, data = null, message = "Question not found.")
            )
        }

        // VOTING 상태에서는 배당률 조회 차단
        if (question.status == "VOTING") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiEnvelope(
                    success = false,
                    data = null,
                    message = "Odds cannot be viewed while voting is in progress."
                )
            )
        }

        val odds = oddsService.calculateOdds(
            poolYes = question.yesBetPool.toDouble(),
            poolNo = question.noBetPool.toDouble(),
            subsidy = 0.0 // 브랜드 지원금은 Question 엔티티에 추가 필요
        )

        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = OddsResponse(
                    questionId = id,
                    yesOdds = odds.yesOdds,
                    noOdds = odds.noOdds,
                    yesPrice = odds.yesPrice,
                    noPrice = odds.noPrice,
                    poolYes = question.yesBetPool,
                    poolNo = question.noBetPool,
                    totalPool = question.totalBetPool
                )
            )
        )
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<ApiEnvelope<HealthStatusResponse>> {
        return ResponseEntity.ok(
            ApiEnvelope(
                success = true,
                data = HealthStatusResponse(
                    status = "OK",
                    service = "predata-backend",
                    version = "1.0.0"
                )
            )
        )
    }

}
