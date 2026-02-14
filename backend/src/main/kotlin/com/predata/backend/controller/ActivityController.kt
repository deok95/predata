package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.domain.ActivityType
import com.predata.backend.service.VoteService
import com.predata.backend.service.BetService
import com.predata.backend.service.QuestionService
import com.predata.backend.service.SettlementService
import com.predata.backend.domain.FinalResult
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
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
    private val bettingSuspensionService: com.predata.backend.service.BettingSuspensionService,
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository
) {

    /**
     * 투표 실행
     */
    @PostMapping("/vote")
    fun vote(@Valid @RequestBody request: VoteRequest, httpRequest: HttpServletRequest): ResponseEntity<ActivityResponse> {
        val clientIp = extractClientIp(httpRequest)
        val response = voteService.vote(request, clientIp)

        // IP 업데이트
        if (response.success) updateMemberIp(request.memberId, clientIp)

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
        val clientIp = extractClientIp(httpRequest)

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

        // 2. 베팅 실행
        val response = betService.bet(request, clientIp)

        // IP 업데이트
        if (response.success) updateMemberIp(request.memberId, clientIp)

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
        val clientIp = extractClientIp(httpRequest)

        // Path와 Body의 betId 일치 확인
        if (betId != request.betId) {
            return ResponseEntity.badRequest().body(
                SellBetResponse(
                    success = false,
                    message = "경로의 betId와 요청의 betId가 일치하지 않습니다."
                )
            )
        }

        val response = betSellService.sellBet(request, clientIp)

        if (response.success) {
            updateMemberIp(request.memberId, clientIp)
        }

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
    }

    private fun updateMemberIp(memberId: Long, ip: String) {
        try {
            memberRepository.findById(memberId).ifPresent { member ->
                member.lastIp = ip
                memberRepository.save(member)
            }
        } catch (_: Exception) { /* IP 업데이트 실패는 무시 */ }
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) return forwarded.split(",").first().trim()
        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) return realIp.trim()
        return request.remoteAddr ?: "unknown"
    }

    /**
     * 모든 질문 조회
     */
    @GetMapping("/questions")
    fun getAllQuestions(): ResponseEntity<List<QuestionResponse>> {
        val questions = questionService.getAllQuestions()
        return ResponseEntity.ok(questions)
    }

    /**
     * 특정 질문 조회
     */
    @GetMapping("/questions/{id}")
    fun getQuestion(@PathVariable id: Long): ResponseEntity<QuestionResponse> {
        val question = questionService.getQuestion(id)
        
        return if (question != null) {
            ResponseEntity.ok(question)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 상태별 질문 조회
     */
    @GetMapping("/questions/status/{status}")
    fun getQuestionsByStatus(@PathVariable status: String): ResponseEntity<List<QuestionResponse>> {
        val questionStatus = com.predata.backend.domain.QuestionStatus.valueOf(status.uppercase())
        val questions = questionService.getQuestionsByStatus(questionStatus)
        return ResponseEntity.ok(questions)
    }

    /**
     * 회원의 투표/베팅 내역 조회
     * GET /api/activities/member/{memberId}?type=VOTE
     */
    @GetMapping("/activities/member/{memberId}")
    fun getActivitiesByMember(
        @PathVariable memberId: Long,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<Map<String, Any?>>> {
        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByMemberIdAndActivityType(memberId, activityType)
        } else {
            activityRepository.findByMemberId(memberId)
        }

        val result = activities.map { a ->
            mapOf(
                "id" to a.id,
                "questionId" to a.questionId,
                "activityType" to a.activityType.name,
                "choice" to a.choice.name,
                "amount" to a.amount,
                "createdAt" to a.createdAt.toString()
            )
        }

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
    ): ResponseEntity<List<Map<String, Any?>>> {
        val activities = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activityRepository.findByQuestionIdAndActivityType(questionId, activityType)
        } else {
            activityRepository.findByQuestionId(questionId)
        }

        val result = activities.map { a ->
            mapOf(
                "id" to a.id,
                "memberId" to a.memberId,
                "questionId" to a.questionId,
                "activityType" to a.activityType.name,
                "choice" to a.choice.name,
                "amount" to a.amount,
                "latencyMs" to a.latencyMs,
                "parentBetId" to a.parentBetId,
                "createdAt" to a.createdAt.toString()
            )
        }

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
    ): ResponseEntity<List<Map<String, Any?>>> {
        val activities = activityRepository.findByMemberIdAndQuestionId(memberId, questionId)

        val filtered = if (type != null) {
            val activityType = ActivityType.valueOf(type.uppercase())
            activities.filter { it.activityType == activityType }
        } else {
            activities
        }

        val result = filtered.map { a ->
            mapOf(
                "id" to a.id,
                "questionId" to a.questionId,
                "activityType" to a.activityType.name,
                "choice" to a.choice.name,
                "amount" to a.amount,
                "latencyMs" to a.latencyMs,
                "parentBetId" to a.parentBetId,
                "createdAt" to a.createdAt.toString()
            )
        }

        return ResponseEntity.ok(result)
    }

    /**
     * 배당률 계산 (실시간)
     * GET /api/questions/{id}/odds
     */
    @GetMapping("/questions/{id}/odds")
    fun getOdds(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val question = questionService.getQuestion(id)
        
        return if (question != null) {
            val odds = calculateOdds(
                poolYes = question.yesBetPool.toDouble(),
                poolNo = question.noBetPool.toDouble(),
                subsidy = 0.0 // 브랜드 지원금은 Question 엔티티에 추가 필요
            )
            
            ResponseEntity.ok(
                mapOf(
                    "questionId" to id,
                    "yesOdds" to odds.yesOdds,
                    "noOdds" to odds.noOdds,
                    "yesPrice" to odds.yesPrice,
                    "noPrice" to odds.noPrice,
                    "poolYes" to question.yesBetPool,
                    "poolNo" to question.noBetPool,
                    "totalPool" to question.totalBetPool
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
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "OK",
                "service" to "predata-backend",
                "version" to "1.0.0"
            )
        )
    }

    /**
     * 정산 시작 (PENDING_SETTLEMENT)
     * POST /api/questions/{id}/settle
     */
    @PostMapping("/questions/{id}/settle")
    fun settleQuestion(
        @PathVariable id: Long,
        @RequestBody request: SettleQuestionRequest
    ): ResponseEntity<Any> {
        val finalResult = FinalResult.valueOf(request.finalResult)
        val result = settlementService.initiateSettlement(id, finalResult, request.sourceUrl)
        return ResponseEntity.ok(result)
    }

    /**
     * 정산 확정 (SETTLED) — 배당금 분배 실행
     * POST /api/questions/{id}/settle/finalize
     */
    @PostMapping("/questions/{id}/settle/finalize")
    fun finalizeSettlement(
        @PathVariable id: Long,
        @RequestBody(required = false) request: FinalizeSettlementRequest?
    ): ResponseEntity<Any> {
        val result = settlementService.finalizeSettlement(id, request?.force ?: false)
        return ResponseEntity.ok(result)
    }

    /**
     * 정산 취소 (OPEN으로 복귀)
     * POST /api/questions/{id}/settle/cancel
     */
    @PostMapping("/questions/{id}/settle/cancel")
    fun cancelSettlement(@PathVariable id: Long): ResponseEntity<Any> {
        val result = settlementService.cancelPendingSettlement(id)
        return ResponseEntity.ok(result)
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

    /**
     * 배당률 계산 로직 (프론트엔드 engine.ts와 동일)
     */
    private fun calculateOdds(poolYes: Double, poolNo: Double, subsidy: Double): OddsResult {
        val effectiveYes = poolYes + (subsidy / 2.0)
        val effectiveNo = poolNo + (subsidy / 2.0)
        val totalPool = effectiveYes + effectiveNo

        if (totalPool == 0.0) {
            return OddsResult(
                yesOdds = "2.00",
                noOdds = "2.00",
                yesPrice = "0.50",
                noPrice = "0.50"
            )
        }

        // 가격의 합이 1.01이 되도록 수수료 1% 적용
        val yesPrice = (effectiveYes / totalPool) * 1.01
        val noPrice = (effectiveNo / totalPool) * 1.01

        return OddsResult(
            yesOdds = String.format("%.2f", 1.0 / yesPrice),
            noOdds = String.format("%.2f", 1.0 / noPrice),
            yesPrice = String.format("%.2f", yesPrice),
            noPrice = String.format("%.2f", noPrice)
        )
    }

    data class OddsResult(
        val yesOdds: String,
        val noOdds: String,
        val yesPrice: String,
        val noPrice: String
    )

    data class SettleQuestionRequest(
        val finalResult: String, // "YES" or "NO"
        val sourceUrl: String? = null // 정산 근거 링크
    )

    data class FinalizeSettlementRequest(
        val force: Boolean = false // true면 이의 제기 기간 무시하고 즉시 확정
    )
}
