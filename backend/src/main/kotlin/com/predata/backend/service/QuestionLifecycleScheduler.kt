package com.predata.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.*
import com.predata.backend.dto.ClaudeApiRequest
import com.predata.backend.dto.ClaudeMessage
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.math.BigDecimal


@Service
@ConditionalOnProperty(
    name = ["app.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class QuestionLifecycleScheduler(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val settlementService: SettlementService,
    private val settlementAutomationService: SettlementAutomationService,
    private val reviewQueueService: SettlementReviewQueueService,
    private val marketPoolRepository: MarketPoolRepository,
    private val swapService: com.predata.backend.service.amm.SwapService,
    @Value("\${anthropic.api.key:}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(QuestionLifecycleScheduler::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    companion object {
        const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
        const val CLAUDE_MODEL = "claude-sonnet-4-20250514"
        const val ANTHROPIC_VERSION = "2023-06-01"

        const val TOTAL_INITIAL_LIQUIDITY = 1000L
        const val MIN_VOTES_FOR_SIGNAL = 10        // 투표 기반 배당을 적용할 최소 투표 수
        const val MIN_ODDS_RATIO = 0.10             // 최소 10%
        const val MAX_ODDS_RATIO = 0.90             // 최대 90%
    }

    fun isDemoMode(): Boolean = apiKey.isBlank()

    /**
     * 1분마다 실행:
     * - VOTING → BREAK (votingEndAt 지남)
     * - BREAK → BETTING (비활성: Phase3 Top3 오픈 경로에서만 전환)
     * - BETTING → SETTLED (bettingEndAt 지남, 정산 트리거)
     * - disputeDeadline 지난 PENDING_SETTLEMENT → 자동 정산 확정
     */
    @Scheduled(cron = "\${app.lifecycle.cron:0 * * * * *}")
    @Transactional
    fun processQuestionLifecycle() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        logger.info("========================================")
        logger.info("[Lifecycle] 스케줄러 실행 시각: $now")
        logger.info("========================================")

        transitionVotingToBreak()
        // VOTE_RESULT 질문 전용: 베팅 종료 시 REVEAL_OPEN → REVEAL_CLOSED (정산 전 먼저 실행)
        transitionRevealOpenToClosed()
        // BREAK -> BETTING은 Phase3 마켓 배치 오픈 경로에서만 처리한다.
        transitionBettingToSettled()
        ensurePoolsForBettingQuestions()
        finalizePastDueSettlements()

        logger.info("[Lifecycle] 스케줄러 실행 완료")
    }

    /**
     * 안전장치:
     * - status=BETTING 인데 market_pools가 없는 질문을 자동 복구(seed)
     */
    fun ensurePoolsForBettingQuestions() {
        val bettingQuestions = questionRepository.findByStatus(QuestionStatus.BETTING)
        if (bettingQuestions.isEmpty()) return

        var fixed = 0
        bettingQuestions.forEach { question ->
            val qid = question.id ?: return@forEach
            val exists = marketPoolRepository.existsById(qid)
            if (exists) return@forEach
            try {
                swapService.seedPool(
                    com.predata.backend.dto.amm.SeedPoolRequest(
                        questionId = qid,
                        seedUsdc = BigDecimal("1000"),
                        feeRate = BigDecimal("0.01"),
                    )
                )
                fixed++
                logger.warn("[Lifecycle] BETTING question #{} had no pool; seeded automatically.", qid)
            } catch (e: Exception) {
                logger.error("[Lifecycle] Failed to auto-seed pool for BETTING question #{}: {}", qid, e.message)
            }
        }
        if (fixed > 0) {
            logger.info("[Lifecycle] Auto-seeded {} missing pools for BETTING questions", fixed)
        }
    }

    /**
     * VOTING → BREAK: 투표 마감 시간이 지난 질문
     */
    fun transitionVotingToBreak() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val votingExpired = questionRepository.findVotingExpiredBefore(now)

        logger.info("[Lifecycle] VOTING → BREAK 체크: 만료된 질문 {}건", votingExpired.size)
        if (votingExpired.isEmpty()) {
            logger.debug("[Lifecycle] VOTING 상태에서 만료된 질문 없음")
            return
        }

        logger.info("[Lifecycle] VOTING 만료 {}건 발견 → BREAK 전환 시작", votingExpired.size)
        votingExpired.forEach { question ->
            try {
                val locked = questionRepository.findByIdWithLock(question.id!!)
                if (locked != null && locked.status == QuestionStatus.VOTING && !locked.votingEndAt.isAfter(now)) {
                    locked.status = QuestionStatus.BREAK
                    // votingPhase는 변경하지 않음: 베팅 오픈 시점에 REVEAL_OPEN으로 전환
                    logger.info("[Lifecycle] 질문 #{} '{}' → BREAK (투표 마감)", locked.id, locked.title)
                    questionRepository.save(locked)
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] Question #{} BREAK transition failed: {}", question.id, e.message)
            }
        }
    }

    /**
     * BREAK → BETTING: 베팅 시작 시간이 도달한 질문 (투표 마감 후 5분)
     */
    fun transitionBreakToBetting() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val breakExpired = questionRepository.findBreakExpiredBefore(now)

        logger.info("[Lifecycle] BREAK → BETTING 체크: 만료된 질문 {}건", breakExpired.size)
        if (breakExpired.isEmpty()) {
            logger.debug("[Lifecycle] BREAK 상태에서 만료된 질문 없음")
            return
        }

        logger.info("[Lifecycle] BREAK 만료 {}건 발견 → BETTING 전환 시작", breakExpired.size)
        breakExpired.forEach { question ->
            try {
                val locked = questionRepository.findByIdWithLock(question.id!!)
                if (locked != null && locked.status == QuestionStatus.BREAK && locked.bettingStartAt.isBefore(now)) {
                    seedInitialPools(locked)
                    locked.status = QuestionStatus.BETTING
                    // VOTE_RESULT: 베팅 오픈과 동시에 reveal 오픈 (베팅 기간 = reveal 기간)
                    if (locked.voteResultSettlement) {
                        locked.votingPhase = VotingPhase.VOTING_REVEAL_OPEN
                        logger.info("[Lifecycle] 질문 #{} '{}' → BETTING + VOTING_REVEAL_OPEN (베팅·reveal 동시 시작)", locked.id, locked.title)
                    } else {
                        logger.info("[Lifecycle] 질문 #{} '{}' → BETTING (초기 풀: YES={}, NO={})", locked.id, locked.title, locked.initialYesPool, locked.initialNoPool)
                    }
                    questionRepository.save(locked)
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] Question #{} BETTING transition failed: {}", question.id, e.message)
            }
        }
    }

    /**
     * BETTING → SETTLED: 베팅 마감 시간이 지난 질문 (정산 트리거)
     * - OPINION 타입: 투표 결과로 자동 정산
     * - VERIFIABLE 타입: 관리자 입력 대기 (자동 정산 안 함)
     */
    fun transitionBettingToSettled() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val bettingExpired = questionRepository.findBettingExpiredBefore(now)

        logger.info("[Lifecycle] BETTING → SETTLED 체크: 만료된 질문 {}건", bettingExpired.size)
        if (bettingExpired.isEmpty()) {
            logger.debug("[Lifecycle] BETTING 상태에서 만료된 질문 없음")
            return
        }

        logger.info("[Lifecycle] BETTING 만료 {}건 발견 → 정산 처리 시작", bettingExpired.size)
        bettingExpired.forEach { question ->
            try {
                val locked = questionRepository.findByIdWithLock(question.id!!)
                if (locked != null && locked.status == QuestionStatus.BETTING && !locked.bettingEndAt.isAfter(now)) {

                    // Auto-first + Fail-safe 자동 정산 시도
                    try {
                        val settlementResult = settlementAutomationService.autoSettleWithVerification(locked.id!!)
                        if (settlementResult != null) {
                            logger.info(
                                "[Lifecycle] 질문 #{} '{}' → Auto-first 자동 정산 완료 (결과: {})",
                                locked.id,
                                locked.title,
                                settlementResult.finalResult
                            )
                            logger.info("[Lifecycle] 배당금 지급 완료: 승자 {}명, 총 배당금 {}P", settlementResult.totalWinners, settlementResult.totalPayout)
                        } else {
                            // VOTE_RESULT reveal 미완료, 또는 VERIFIABLE이 아닌 경우: 큐 등록 불필요
                            val shouldEnqueue = when {
                                locked.voteResultSettlement && locked.votingPhase != VotingPhase.VOTING_REVEAL_CLOSED -> false
                                !locked.voteResultSettlement && locked.marketType != MarketType.VERIFIABLE -> false
                                else -> true
                            }
                            if (shouldEnqueue) {
                                reviewQueueService.enqueueOrUpdate(
                                    questionId = locked.id!!,
                                    reasonCode = SettlementReviewReasonCode.SOURCE_UNAVAILABLE,
                                    reasonDetail = "autoSettle returned null (phase=${locked.votingPhase})",
                                )
                                logger.info("[Lifecycle] 질문 #{} 재시도 큐 등록", locked.id)
                            } else {
                                logger.info("[Lifecycle] 질문 #{} 정산 조건 미충족, 다음 사이클 대기", locked.id)
                            }
                        }
                    } catch (settlementError: Exception) {
                        logger.error(
                            "[Lifecycle] 질문 #{} 자동 정산 예외: {}",
                            locked.id,
                            settlementError.message
                        )
                        reviewQueueService.enqueueOrUpdate(
                            questionId = locked.id!!,
                            reasonCode = SettlementReviewReasonCode.EXCEPTION,
                            reasonDetail = settlementError.message?.take(500),
                        )
                        logger.warn("[Lifecycle] 질문 #{} 예외 발생 → 재시도 큐 등록", locked.id)
                    }
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] Question #{} SETTLED transition failed: {}", question.id, e.message)
            }
        }
    }

    fun finalizePastDueSettlements() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val pendingQuestions = questionRepository.findPendingSettlementPastDeadline(now)
        if (pendingQuestions.isEmpty()) return

        logger.info("[Lifecycle] 이의제기 만료 PENDING_SETTLEMENT 질문 {}건 발견", pendingQuestions.size)
        pendingQuestions.forEach { question ->
            try {
                val result = settlementService.finalizeSettlement(question.id!!)
                logger.info("[Lifecycle] 질문 #{} 자동 정산 확정 (배당: {}P)", question.id, result.totalPayout)
            } catch (e: Exception) {
                logger.error("[Lifecycle] Question #{} auto-settlement failed: {}", question.id, e.message)
            }
        }
    }

    /**
     * VOTE_RESULT 질문 전용: REVEAL_OPEN → REVEAL_CLOSED
     * 베팅 종료(bettingEndAt = revealWindowEndAt)가 지난 BETTING 상태 질문을 VOTING_REVEAL_CLOSED로 전환.
     * transitionBettingToSettled() 앞에 실행되어 자동 정산이 즉시 가능하도록 함.
     */
    fun transitionRevealOpenToClosed() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val revealExpired = questionRepository.findVoteResultQuestionsRevealExpired(now)
        if (revealExpired.isEmpty()) return

        logger.info("[Lifecycle] REVEAL_OPEN → REVEAL_CLOSED 체크: {}건", revealExpired.size)
        revealExpired.forEach { question ->
            try {
                val locked = questionRepository.findByIdWithLock(question.id!!)
                if (locked != null
                    && locked.voteResultSettlement
                    && locked.votingPhase == VotingPhase.VOTING_REVEAL_OPEN
                    && locked.revealWindowEndAt != null
                    && !locked.revealWindowEndAt!!.isAfter(now)) {
                    locked.votingPhase = VotingPhase.VOTING_REVEAL_CLOSED
                    questionRepository.save(locked)
                    logger.info("[Lifecycle] 질문 #{} '{}' → VOTING_REVEAL_CLOSED (베팅·reveal 동시 마감)", locked.id, locked.title)
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] Question #{} REVEAL_CLOSED transition failed: {}", question.id, e.message)
            }
        }
    }

    /**
     * 초기 풀 설정
     * - 정책 변경: 투표 결과로 시딩하지 않고 중립(50:50)에서 시작
     * - 이유: 서버도 베팅 종료 전까지 투표 결과를 알 수 없어야 함
     */
    private fun seedInitialPools(question: Question) {
        // 균등 배분 (50:50 = 500:500)
        question.initialYesPool = TOTAL_INITIAL_LIQUIDITY / 2
        question.initialNoPool = TOTAL_INITIAL_LIQUIDITY / 2
        logger.info(
            "[Lifecycle] 질문 #{}: 중립 초기 배당 (50:50)",
            question.id
        )

        question.yesBetPool = question.initialYesPool
        question.noBetPool = question.initialNoPool
        question.totalBetPool = TOTAL_INITIAL_LIQUIDITY
    }

    /**
     * VERIFIABLE 질문 자동 정산
     * - 데모 모드: 투표 결과로 자동 정산
     * - API 모드: Claude API로 결과 판정
     */
    private fun settleVerifiableQuestion(question: Question) {
        if (isDemoMode()) {
            // 데모 모드: 투표 결과로 자동 정산 (OPINION과 동일)
            settleByVoteResult(question, "AUTO_SETTLEMENT_VERIFIABLE_DEMO")
        } else {
            // API 모드: Claude로 판단
            val judgment = callClaudeForJudgment(question)
            when (judgment) {
                "YES" -> {
                    logger.info("[Lifecycle] VERIFIABLE 질문 #{} → Claude 판정: YES", question.id)
                    autoSettle(question, FinalResult.YES, "AUTO_SETTLEMENT_VERIFIABLE_AI")
                }
                "NO" -> {
                    logger.info("[Lifecycle] VERIFIABLE 질문 #{} → Claude 판정: NO", question.id)
                    autoSettle(question, FinalResult.NO, "AUTO_SETTLEMENT_VERIFIABLE_AI")
                }
                else -> {
                    // UNKNOWN: 관리자 수동 입력 대기
                    logger.info(
                        "[Lifecycle] VERIFIABLE 질문 #{} '{}' → Claude 판정 불가 (UNKNOWN), 관리자 입력 대기",
                        question.id,
                        question.title
                    )
                }
            }
        }
    }

    /**
     * 투표 결과로 정산
     */
    private fun settleByVoteResult(question: Question, sourceUrl: String) {
        try {
            val votes = activityRepository.findByQuestionIdAndActivityType(
                question.id!!,
                ActivityType.VOTE
            )

            val yesVotes = votes.count { it.choice == Choice.YES }
            val noVotes = votes.count { it.choice == Choice.NO }

            val result = if (yesVotes > noVotes) FinalResult.YES else FinalResult.NO

            val settlementResult = settlementService.initiateSettlement(
                questionId = question.id!!,
                finalResult = result,
                sourceUrl = sourceUrl
            )
            logger.info(
                "[Lifecycle] VERIFIABLE 질문 #{} '{}' → 투표 결과로 자동 정산 (YES: {}, NO: {}, 결과: {})",
                question.id,
                question.title,
                yesVotes,
                noVotes,
                result
            )
            logger.info("[Lifecycle] Settlement message: {}", settlementResult.message)

            // 즉시 배당금 지급
            val finalResult = settlementService.finalizeSettlement(
                questionId = question.id!!,
                skipDeadlineCheck = true
            )
            logger.info("[Lifecycle] 배당금 지급 완료: 승자 {}명, 총 배당금 {}P", finalResult.totalWinners, finalResult.totalPayout)
        } catch (e: Exception) {
            logger.error("[Lifecycle] VERIFIABLE question #{} settlement failed: {}", question.id, e.message)
        }
    }

    /**
     * 자동 정산 실행
     */
    private fun autoSettle(question: Question, result: FinalResult, sourceUrl: String) {
        try {
            val settlementResult = settlementService.initiateSettlement(
                questionId = question.id!!,
                finalResult = result,
                sourceUrl = sourceUrl
            )
            logger.info("[Lifecycle] Settlement message: {}", settlementResult.message)

            // 즉시 배당금 지급
            val finalResult = settlementService.finalizeSettlement(
                questionId = question.id!!,
                skipDeadlineCheck = true
            )
            logger.info("[Lifecycle] 배당금 지급 완료: 승자 {}명, 총 배당금 {}P", finalResult.totalWinners, finalResult.totalPayout)
        } catch (e: Exception) {
            logger.error("[Lifecycle] Question #{} auto-settlement failed: {}", question.id, e.message)
        }
    }

    /**
     * Claude API로 질문 결과 판정
     * @return "YES", "NO", or "UNKNOWN"
     */
    private fun callClaudeForJudgment(question: Question): String {
        val prompt = buildJudgmentPrompt(question)

        val request = ClaudeApiRequest(
            model = CLAUDE_MODEL,
            max_tokens = 64,
            messages = listOf(ClaudeMessage(role = "user", content = prompt))
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-api-key", apiKey)
            set("anthropic-version", ANTHROPIC_VERSION)
        }

        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        try {
            val response = restTemplate.exchange(
                CLAUDE_API_URL,
                HttpMethod.POST,
                entity,
                String::class.java
            )

            val jsonNode = objectMapper.readTree(response.body)
            val content = jsonNode.get("content")?.get(0)?.get("text")?.asText()?.trim()?.uppercase()

            logger.info("[Lifecycle] Claude 응답: $content (질문: ${question.title})")

            return when {
                content?.contains("YES") == true -> "YES"
                content?.contains("NO") == true -> "NO"
                else -> "UNKNOWN"
            }

        } catch (e: Exception) {
            logger.error("[Lifecycle] Claude API call failed: ${e.message}")
            return "UNKNOWN"
        }
    }

    /**
     * 결과 판정용 프롬프트 생성
     */
    private fun buildJudgmentPrompt(question: Question): String {
        val today = LocalDate.now(ZoneOffset.UTC)
        return """
질문: ${question.title}
현재 날짜: $today
이 질문의 결과가 YES인지 NO인지 판단해줘.
아직 모르면 UNKNOWN.
YES, NO, UNKNOWN 중 하나만 답해.
        """.trimIndent()
    }
}
