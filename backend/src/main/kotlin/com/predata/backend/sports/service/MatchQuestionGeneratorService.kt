package com.predata.backend.sports.service

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.domain.QuestionPhase
import com.predata.backend.sports.repository.MatchRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class MatchQuestionGeneratorService(
    private val matchRepository: MatchRepository,
    private val questionRepository: QuestionRepository,
    @Value("\${sports.football-data.fetch-window-days:7}")
    private val fetchWindowDays: Long
) {

    private val logger = LoggerFactory.getLogger(MatchQuestionGeneratorService::class.java)

    /**
     * SCHEDULED 상태 Match에 대해 Question 자동 생성
     * - 이미 해당 Match에 Question 있으면 skip (중복 방지)
     * - phase = UPCOMING, status = VOTING
     * - QuestionPhaseScheduler가 D-2 → VOTING, D-1 → BETTING 자동 전환
     */
    @Transactional
    fun generateQuestions(): MatchQuestionGenerateResult {
        val start = LocalDateTime.now(ZoneOffset.UTC)
        val end = start.plusDays(fetchWindowDays)
        val matches = matchRepository.findByMatchStatusAndMatchTimeBetween(
            MatchStatus.SCHEDULED,
            start,
            end
        ).sortedBy { it.matchTime }
        var created = 0
        var skipped = 0

        for (match in matches) {
            val matchId = match.id ?: continue

            if (questionRepository.findByMatchId(matchId).isNotEmpty()) {
                skipped++
                continue
            }

            val question = createQuestion(match)
            questionRepository.save(question)
            created++

            logger.info("[QuestionGen] 생성: {} vs {} (match={})", match.homeTeam, match.awayTeam, matchId)
        }

        logger.info("[QuestionGen] 완료 - 생성: {}건, 스킵: {}건", created, skipped)
        return MatchQuestionGenerateResult(created, skipped)
    }

    private fun createQuestion(match: Match): Question {
        val leagueName = match.league.name
        val title = "[$leagueName] ${match.homeTeam} vs ${match.awayTeam} - 승자는?"

        val matchTime = match.matchTime
        val votingEndAt = matchTime.minusHours(24)
        val bettingStartAt = votingEndAt
        val bettingEndAt = matchTime.plusHours(2)
        val expiredAt = bettingEndAt

        return Question(
            title = title,
            category = "SPORTS",
            categoryWeight = BigDecimal("1.00"),
            status = QuestionStatus.VOTING,
            type = QuestionType.VERIFIABLE,
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            expiredAt = expiredAt,
            finalResult = FinalResult.PENDING,
            phase = QuestionPhase.UPCOMING,
            match = match
        )
    }
}

data class MatchQuestionGenerateResult(
    val created: Int,
    val skipped: Int
)
