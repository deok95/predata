package com.predata.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.dto.ClaudeApiRequest
import com.predata.backend.dto.ClaudeMessage
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.ActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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

@Service
class QuestionLifecycleScheduler(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val settlementService: SettlementService,
    @Value("\${anthropic.api.key:}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(QuestionLifecycleScheduler::class.java)
    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    companion object {
        const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
        const val CLAUDE_MODEL = "claude-sonnet-4-20250514"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }

    fun isDemoMode(): Boolean = apiKey.isBlank()

    /**
     * 1분마다 실행:
     * - VOTING → BREAK (votingEndAt 지남)
     * - BREAK → BETTING (bettingStartAt 도달)
     * - BETTING → SETTLED (bettingEndAt 지남, 정산 트리거)
     * - disputeDeadline 지난 PENDING_SETTLEMENT → 자동 정산 확정
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun processQuestionLifecycle() {
        val now = LocalDateTime.now()
        logger.info("========================================")
        logger.info("[Lifecycle] 스케줄러 실행 시각: $now")
        logger.info("========================================")

        transitionVotingToBreak()
        transitionBreakToBetting()
        transitionBettingToSettled()
        finalizePastDueSettlements()

        logger.info("[Lifecycle] 스케줄러 실행 완료")
    }

    /**
     * VOTING → BREAK: 투표 마감 시간이 지난 질문
     */
    fun transitionVotingToBreak() {
        val now = LocalDateTime.now()
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
                if (locked != null && locked.status == QuestionStatus.VOTING && locked.votingEndAt.isBefore(now)) {
                    locked.status = QuestionStatus.BREAK
                    questionRepository.save(locked)
                    logger.info("[Lifecycle] 질문 #{} '{}' → BREAK (투표 마감)", locked.id, locked.title)
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] 질문 #{} BREAK 전환 실패: {}", question.id, e.message)
            }
        }
    }

    /**
     * BREAK → BETTING: 베팅 시작 시간이 도달한 질문 (투표 마감 후 5분)
     */
    fun transitionBreakToBetting() {
        val now = LocalDateTime.now()
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
                    locked.status = QuestionStatus.BETTING
                    questionRepository.save(locked)
                    logger.info("[Lifecycle] 질문 #{} '{}' → BETTING (베팅 시작)", locked.id, locked.title)
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] 질문 #{} BETTING 전환 실패: {}", question.id, e.message)
            }
        }
    }

    /**
     * BETTING → SETTLED: 베팅 마감 시간이 지난 질문 (정산 트리거)
     * - OPINION 타입: 투표 결과로 자동 정산
     * - VERIFIABLE 타입: 관리자 입력 대기 (자동 정산 안 함)
     */
    fun transitionBettingToSettled() {
        val now = LocalDateTime.now()
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
                if (locked != null && locked.status == QuestionStatus.BETTING && locked.bettingEndAt.isBefore(now)) {

                    // 어댑터 기반 자동 정산 (OPINION, VERIFIABLE 모두 통합)
                    try {
                        val settlementResult = settlementService.initiateSettlementAuto(locked.id!!)
                        logger.info(
                            "[Lifecycle] 질문 #{} '{}' (타입: {}) → 어댑터 기반 자동 정산 완료 (결과: {})",
                            locked.id,
                            locked.title,
                            locked.type,
                            settlementResult.finalResult
                        )
                        logger.info("[Lifecycle] 정산 메시지: {}", settlementResult.message)

                        // 즉시 배당금 지급
                        val finalResult = settlementService.finalizeSettlement(
                            questionId = locked.id!!,
                            skipDeadlineCheck = true
                        )
                        logger.info("[Lifecycle] 배당금 지급 완료: 승자 {}명, 총 배당금 {}P", finalResult.totalWinners, finalResult.totalPayout)
                    } catch (settlementError: Exception) {
                        logger.error(
                            "[Lifecycle] 질문 #{} 어댑터 기반 정산 실패: {}",
                            locked.id,
                            settlementError.message
                        )
                        // 실패 시 수동 정산 대기 상태로 설정
                        locked.status = QuestionStatus.SETTLED
                        questionRepository.save(locked)
                    }
                }
            } catch (e: Exception) {
                logger.error("[Lifecycle] 질문 #{} SETTLED 전환 실패: {}", question.id, e.message)
            }
        }
    }

    fun finalizePastDueSettlements() {
        val now = LocalDateTime.now()
        val pendingQuestions = questionRepository.findPendingSettlementPastDeadline(now)
        if (pendingQuestions.isEmpty()) return

        logger.info("[Lifecycle] 이의제기 만료 PENDING_SETTLEMENT 질문 {}건 발견", pendingQuestions.size)
        pendingQuestions.forEach { question ->
            try {
                val result = settlementService.finalizeSettlement(question.id!!)
                logger.info("[Lifecycle] 질문 #{} 자동 정산 확정 (배당: {}P)", question.id, result.totalPayout)
            } catch (e: Exception) {
                logger.error("[Lifecycle] 질문 #{} 자동 정산 실패: {}", question.id, e.message)
            }
        }
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
            logger.info("[Lifecycle] 정산 메시지: {}", settlementResult.message)

            // 즉시 배당금 지급
            val finalResult = settlementService.finalizeSettlement(
                questionId = question.id!!,
                skipDeadlineCheck = true
            )
            logger.info("[Lifecycle] 배당금 지급 완료: 승자 {}명, 총 배당금 {}P", finalResult.totalWinners, finalResult.totalPayout)
        } catch (e: Exception) {
            logger.error("[Lifecycle] VERIFIABLE 질문 #{} 정산 실패: {}", question.id, e.message)
            question.status = QuestionStatus.SETTLED
            questionRepository.save(question)
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
            logger.info("[Lifecycle] 정산 메시지: {}", settlementResult.message)

            // 즉시 배당금 지급
            val finalResult = settlementService.finalizeSettlement(
                questionId = question.id!!,
                skipDeadlineCheck = true
            )
            logger.info("[Lifecycle] 배당금 지급 완료: 승자 {}명, 총 배당금 {}P", finalResult.totalWinners, finalResult.totalPayout)
        } catch (e: Exception) {
            logger.error("[Lifecycle] 질문 #{} 자동 정산 실패: {}", question.id, e.message)
            question.status = QuestionStatus.SETTLED
            questionRepository.save(question)
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
            logger.error("[Lifecycle] Claude API 호출 실패: ${e.message}")
            return "UNKNOWN"
        }
    }

    /**
     * 결과 판정용 프롬프트 생성
     */
    private fun buildJudgmentPrompt(question: Question): String {
        val today = LocalDate.now()
        return """
질문: ${question.title}
현재 날짜: $today
이 질문의 결과가 YES인지 NO인지 판단해줘.
아직 모르면 UNKNOWN.
YES, NO, UNKNOWN 중 하나만 답해.
        """.trimIndent()
    }
}
