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
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ActivityController(
    private val voteService: VoteService,
    private val betService: BetService,
    private val betSellService: com.predata.backend.service.BetSellService,
    private val questionService: QuestionService,
    private val settlementService: SettlementService,
    private val oddsService: OddsService,
    private val clientIpService: ClientIpService,
    private val bettingSuspensionService: com.predata.backend.service.BettingSuspensionService,
    private val activityRepository: ActivityRepository
) {

    /**
     * 투표 실행
     */
    @PostMapping("/vote")
    fun vote(@Valid @RequestBody request: VoteRequest, httpRequest: HttpServletRequest): ResponseEntity<Any> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        val clientIp = clientIpService.extractClientIp(httpRequest)
        val response = voteService.vote(authenticatedMemberId, request, clientIp)

        // IP 업데이트
        if (response.success) clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)

        return if (response.success) {
            ResponseEntity.ok(ApiEnvelope.ok(response))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    /**
     * 베팅 실행 (AMM_FPMM)
     */
    @PostMapping("/bet")
    fun bet(@Valid @RequestBody request: BetRequest, httpRequest: HttpServletRequest): ResponseEntity<Any> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()
        val clientIp = clientIpService.extractClientIp(httpRequest)

        val response = betService.bet(authenticatedMemberId, request, clientIp)

        if (response.success) clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)

        return if (response.success) {
            ResponseEntity.ok(ApiEnvelope.ok(response))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
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
        return ResponseEntity.ok(ApiEnvelope.ok(questions))
    }

    /**
     * 특정 질문 조회
     */
    @GetMapping("/questions/{id}")
    fun getQuestion(@PathVariable id: Long): ResponseEntity<ApiEnvelope<QuestionResponse>> {
        val question = questionService.getQuestion(id)

        return if (question != null) {
            ResponseEntity.ok(ApiEnvelope.ok(question))
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
            ResponseEntity.ok(ApiEnvelope.ok(true))
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
        return ResponseEntity.ok(ApiEnvelope.ok(questions))
    }

    /**
     * 본인의 투표/베팅 내역 조회 (JWT 인증 사용)
     * GET /api/activities/me?type=VOTE
     */
    @GetMapping("/activities/me")
    fun getMyActivities(
        @RequestParam(required = false) type: String?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<List<MemberActivityView>>> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByMemberIdAndActivityType(authenticatedMemberId, activityType)
        } else {
            activityRepository.findByMemberId(authenticatedMemberId)
        }

        val result = activities.map(ActivityViewAssembler::toMemberActivityView)

        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 특정 질문의 모든 활동 조회
     * GET /api/activities/question/{questionId}
     */
    @GetMapping("/activities/question/{questionId}")
    fun getActivitiesByQuestion(
        @PathVariable questionId: Long,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<ApiEnvelope<List<QuestionActivityView>>> {
        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByQuestionIdAndActivityType(questionId, activityType)
        } else {
            activityRepository.findByQuestionId(questionId)
        }

        val result = activities.map(ActivityViewAssembler::toQuestionActivityView)

        return ResponseEntity.ok(ApiEnvelope.ok(result))
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
    ): ResponseEntity<ApiEnvelope<List<MemberQuestionActivityView>>> {
        val authenticatedMemberId = httpRequest.authenticatedMemberId()

        val activities = activityRepository.findByMemberIdAndQuestionId(authenticatedMemberId, questionId)

        val filtered = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activities.filter { it.activityType == activityType }
        } else {
            activities
        }

        val result = filtered.map(ActivityViewAssembler::toMemberQuestionActivityView)

        return ResponseEntity.ok(ApiEnvelope.ok(result))
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

        val response = OddsResponse(
            questionId = id,
            yesOdds = odds.yesOdds,
            noOdds = odds.noOdds,
            yesPrice = odds.yesPrice,
            noPrice = odds.noPrice,
            poolYes = question.yesBetPool,
            poolNo = question.noBetPool,
            totalPool = question.totalBetPool
        )
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<ApiEnvelope<HealthStatusResponse>> {
        val response = HealthStatusResponse(
            status = "OK",
            service = "predata-backend",
            version = "1.0.0"
        )
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

}
