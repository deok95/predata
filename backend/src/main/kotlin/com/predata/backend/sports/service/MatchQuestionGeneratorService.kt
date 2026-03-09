package com.predata.backend.sports.service

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.domain.QuestionPhase
import com.predata.backend.sports.provider.football.FootballLeagueCatalog
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
     * 종료 전 상태(SCHEDULED/LIVE/HALFTIME) Match에 대해 Question 자동 생성
     * - 이미 해당 Match에 Question 있으면 skip (중복 방지)
     * - phase = UPCOMING, status = VOTING
     * - QuestionPhaseScheduler가 D-2 → VOTING, D-1 → BETTING 자동 전환
     */
    @Transactional
    fun generateQuestions(): MatchQuestionGenerateResult {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        // 이미 시작된 경기(LIVE/HT)도 질문 생성 대상에 포함하기 위해 lookback 창을 둔다.
        val start = now.minusDays(1)
        val end = now.plusDays(fetchWindowDays)
        val openStatuses = listOf(
            MatchStatus.SCHEDULED,
            MatchStatus.LIVE,
            MatchStatus.HALFTIME
        )
        val matches = matchRepository.findByMatchStatusInAndMatchTimeBetween(
            openStatuses,
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
        val leagueCode = match.league.externalLeagueId
        val subCategory = FootballLeagueCatalog.subCategoryByCode(leagueCode)
        val title = "[$leagueName] ${match.homeTeam} vs ${match.awayTeam} - Who will win?"
        val isLiveMatch = match.matchStatus == MatchStatus.LIVE || match.matchStatus == MatchStatus.HALFTIME

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val matchTime = match.matchTime
        val votingEndAt = now
        val bettingStartAt = now
        val defaultBettingEndAt = matchTime.plusHours(2)
        // 라이브 경기에서 늦게 생성되더라도 최소 유효 베팅 시간이 남도록 보정.
        val bettingEndAt = if (defaultBettingEndAt.isAfter(now.plusMinutes(10))) {
            defaultBettingEndAt
        } else {
            now.plusMinutes(10)
        }
        val expiredAt = bettingEndAt

        return Question(
            title = title,
            category = if (isLiveMatch) "LIVE" else "SPORTS",
            categoryWeight = BigDecimal("1.00"),
            status = QuestionStatus.BETTING,
            type = QuestionType.VERIFIABLE,
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            expiredAt = expiredAt,
            tagsJson = """["football","$subCategory","${leagueCode ?: "UNKNOWN"}"]""",
            finalResult = FinalResult.PENDING,
            phase = if (isLiveMatch) QuestionPhase.LIVE else QuestionPhase.BETTING,
            match = match
        )
    }
}

data class MatchQuestionGenerateResult(
    val created: Int,
    val skipped: Int
)
