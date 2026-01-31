package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.service.VoteService
import com.predata.backend.service.BetService
import com.predata.backend.service.QuestionService
import com.predata.backend.service.TicketService
import com.predata.backend.service.SettlementService
import com.predata.backend.domain.FinalResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:3000"]) // Next.js 프론트엔드
class ActivityController(
    private val voteService: VoteService,
    private val betService: BetService,
    private val questionService: QuestionService,
    private val ticketService: TicketService,
    private val settlementService: SettlementService
) {

    /**
     * 투표 실행
     */
    @PostMapping("/vote")
    fun vote(@RequestBody request: VoteRequest): ResponseEntity<ActivityResponse> {
        val response = voteService.vote(request)
        
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
    fun bet(@RequestBody request: BetRequest): ResponseEntity<ActivityResponse> {
        val response = betService.bet(request)
        
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
        val questions = questionService.getQuestionsByStatus(status)
        return ResponseEntity.ok(questions)
    }

    /**
     * 남은 티켓 조회
     */
    @GetMapping("/tickets/{memberId}")
    fun getTicketStatus(@PathVariable memberId: Long): ResponseEntity<TicketStatusResponse> {
        val ticket = ticketService.getOrCreateTodayTicket(memberId)
        
        return ResponseEntity.ok(
            TicketStatusResponse(
                remainingCount = ticket.remainingCount,
                resetDate = ticket.resetDate.toString()
            )
        )
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
     * 질문 결과 확정 및 정산
     * POST /api/questions/{id}/settle
     */
    @PostMapping("/questions/{id}/settle")
    fun settleQuestion(
        @PathVariable id: Long,
        @RequestBody request: SettleQuestionRequest
    ): ResponseEntity<Any> {
        return try {
            val finalResult = FinalResult.valueOf(request.finalResult)
            val result = settlementService.settleQuestion(id, finalResult)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "잘못된 요청입니다.")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (e.message ?: "이미 정산되었습니다.")))
        }
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
        val finalResult: String // "YES" or "NO"
    )
}
