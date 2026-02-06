package com.predata.backend.service

import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.ActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QuestionLifecycleScheduler(
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val settlementService: SettlementService
) {
    private val logger = LoggerFactory.getLogger(QuestionLifecycleScheduler::class.java)

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

                    when (locked.type) {
                        QuestionType.OPINION -> {
                            // OPINION 타입: 투표 결과로 자동 정산
                            try {
                                val votes = activityRepository.findByQuestionIdAndActivityType(
                                    locked.id!!,
                                    ActivityType.VOTE
                                )

                                val yesVotes = votes.count { it.choice == Choice.YES }
                                val noVotes = votes.count { it.choice == Choice.NO }

                                val result = if (yesVotes > noVotes) FinalResult.YES else FinalResult.NO

                                val settlementResult = settlementService.initiateSettlement(
                                    questionId = locked.id!!,
                                    finalResult = result,
                                    sourceUrl = "AUTO_SETTLEMENT_OPINION"
                                )
                                logger.info(
                                    "[Lifecycle] OPINION 질문 #{} '{}' → 투표 결과로 자동 정산 (YES: {}, NO: {}, 결과: {})",
                                    locked.id,
                                    locked.title,
                                    yesVotes,
                                    noVotes,
                                    result
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
                                    "[Lifecycle] OPINION 질문 #{} 정산 시작 실패: {}",
                                    locked.id,
                                    settlementError.message
                                )
                                locked.status = QuestionStatus.SETTLED
                                questionRepository.save(locked)
                            }
                        }

                        QuestionType.VERIFIABLE -> {
                            // VERIFIABLE 타입: 관리자 입력 대기, 상태만 SETTLED로 변경
                            logger.info(
                                "[Lifecycle] VERIFIABLE 질문 #{} '{}' → 관리자 결과 입력 대기",
                                locked.id,
                                locked.title
                            )
                            // 자동 정산 안 함 - 관리자가 수동으로 결과 입력할 때까지 대기
                        }
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
}
