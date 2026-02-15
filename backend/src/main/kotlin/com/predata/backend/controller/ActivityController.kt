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
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001"])
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
    fun vote(@Valid @RequestBody request: VoteRequest, httpRequest: HttpServletRequest): ResponseEntity<ActivityResponse> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ActivityResponse(success = false, message = "인증이 필요합니다.")
            )

        val clientIp = clientIpService.extractClientIp(httpRequest)
        // request의 memberId는 무시하고 JWT의 memberId 사용
        val safeRequest = request.copy(memberId = authenticatedMemberId)
        val response = voteService.vote(safeRequest, clientIp)

        // IP 업데이트
        if (response.success) clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    /**
     * 베팅 실행
     */
    @PostMapping("/bet")
    fun bet(@Valid @RequestBody request: BetRequest, httpRequest: HttpServletRequest): ResponseEntity<ActivityResponse> {
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ActivityResponse(success = false, message = "인증이 필요합니다.")
            )

        val clientIp = clientIpService.extractClientIp(httpRequest)

        // 1. 베팅 일시 중지 체크
        val suspensionStatus = bettingSuspensionService.isBettingSuspendedByQuestionId(request.questionId)
        if (suspensionStatus.suspended) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ActivityResponse(
                    success = false,
                    message = "⚠️ 골 직후 베팅이 일시 중지되었습니다. ${suspensionStatus.remainingSeconds}초 후 재개됩니다."
                )
            )
        }

        // 2. 베팅 실행 (request의 memberId는 무시하고 JWT의 memberId 사용)
        val safeRequest = request.copy(memberId = authenticatedMemberId)
        val response = betService.bet(safeRequest, clientIp)

        // IP 업데이트
        if (response.success) clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)

        return if (response.success) {
            ResponseEntity.ok(response)
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
        // JWT에서 인증된 memberId 가져오기 (IDOR 방지)
        val authenticatedMemberId = httpRequest.getAttribute(com.predata.backend.config.JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                SellBetResponse(success = false, message = "인증이 필요합니다.")
            )

        val clientIp = clientIpService.extractClientIp(httpRequest)

        // Path와 Body의 betId 일치 확인
        if (betId != request.betId) {
            return ResponseEntity.badRequest().body(
                SellBetResponse(
                    success = false,
                    message = "경로의 betId와 요청의 betId가 일치하지 않습니다."
                )
            )
        }

        // request의 memberId는 무시하고 JWT의 memberId 사용
        val safeRequest = request.copy(memberId = authenticatedMemberId)
        val response = betSellService.sellBet(safeRequest, clientIp)

        if (response.success) {
            clientIpService.updateMemberLastIp(authenticatedMemberId, clientIp)
        }

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
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
            ResponseEntity.notFound().build()
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
     * 회원의 투표/베팅 내역 조회
     * GET /api/activities/member/{memberId}?type=VOTE
     */
    @GetMapping("/activities/member/{memberId}")
    fun getActivitiesByMember(
        @PathVariable memberId: Long,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<MemberActivityView>> {
        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByMemberIdAndActivityType(memberId, activityType)
        } else {
            activityRepository.findByMemberId(memberId)
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
     * 특정 회원의 특정 질문에 대한 활동 조회
     * GET /api/activities/member/{memberId}/question/{questionId}
     */
    @GetMapping("/activities/member/{memberId}/question/{questionId}")
    fun getActivitiesByMemberAndQuestion(
        @PathVariable memberId: Long,
        @PathVariable questionId: Long,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<MemberQuestionActivityView>> {
        val activities = activityRepository.findByMemberIdAndQuestionId(memberId, questionId)

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
        
        return if (question != null) {
            val odds = oddsService.calculateOdds(
                poolYes = question.yesBetPool.toDouble(),
                poolNo = question.noBetPool.toDouble(),
                subsidy = 0.0 // 브랜드 지원금은 Question 엔티티에 추가 필요
            )
            
            ResponseEntity.ok(
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
        } else {
            ResponseEntity.notFound().build()
        }
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

    /**
     * 정산 내역 조회
     * GET /api/settlements/history/{memberId}
     */
    @GetMapping("/settlements/history/{memberId}")
    fun getSettlementHistory(@PathVariable memberId: Long): ResponseEntity<List<com.predata.backend.service.SettlementHistoryItem>> {
        val history = settlementService.getSettlementHistory(memberId)
        return ResponseEntity.ok(history)
    }

}
