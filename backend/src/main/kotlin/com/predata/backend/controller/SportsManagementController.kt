package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import com.predata.backend.sports.repository.MatchRepository
import com.predata.backend.sports.scheduler.LivePollResult
import com.predata.backend.sports.scheduler.MatchSyncResult
import com.predata.backend.sports.scheduler.MatchSyncScheduler
import com.predata.backend.sports.service.MatchQuestionGenerateResult
import com.predata.backend.sports.service.MatchQuestionGeneratorService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/admin/sports")
class SportsManagementController(
    private val matchSyncScheduler: MatchSyncScheduler,
    private val matchRepository: MatchRepository,
    private val matchQuestionGeneratorService: MatchQuestionGeneratorService,
    private val questionRepository: QuestionRepository,
    @Value("\${sports.football-data.fetch-window-days:7}")
    private val fetchWindowDays: Long
) {

    /**
     * 수동으로 경기 동기화 + 질문 자동 생성 트리거
     * POST /api/admin/sports/generate
     */
    @PostMapping("/generate")
    fun manualGenerate(): ResponseEntity<ApiEnvelope<MatchSyncResult>> {
        val result = matchSyncScheduler.syncUpcomingMatches()
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 수동으로 LIVE 경기 폴링 실행
     * POST /api/admin/sports/update-results
     */
    @PostMapping("/update-results")
    fun manualUpdateResults(): ResponseEntity<ApiEnvelope<LivePollResult>> {
        val result = matchSyncScheduler.pollLiveMatches()
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 현재 LIVE 경기 목록 조회
     * GET /api/admin/sports/live
     */
    @GetMapping("/live")
    fun getLiveMatches(): ResponseEntity<ApiEnvelope<List<LiveMatchInfo>>> {
        val liveMatches = matchRepository.findByMatchStatus(MatchStatus.LIVE) +
            matchRepository.findByMatchStatus(MatchStatus.HALFTIME)
        val questionIdByMatch = buildQuestionIdByMatch()
        val liveMatchInfos = liveMatches
            .distinctBy { it.id }
            .map { match -> toLiveMatchInfo(match, questionIdByMatch[match.id]) }
        return ResponseEntity.ok(ApiEnvelope.ok(liveMatchInfos))
    }

    /**
     * 예정된 경기 목록 조회
     * GET /api/admin/sports/upcoming
     */
    @GetMapping("/upcoming")
    fun getUpcomingMatches(): ResponseEntity<ApiEnvelope<List<UpcomingMatchInfo>>> {
        val start = LocalDateTime.now(ZoneOffset.UTC)
        val end = start.plusDays(fetchWindowDays)
        val questionIdByMatch = buildQuestionIdByMatch()
        val upcomingMatches = matchRepository.findByMatchStatusAndMatchTimeBetween(
            MatchStatus.SCHEDULED,
            start,
            end
        ).sortedBy { it.matchTime }
            .map { match ->
                UpcomingMatchInfo(
                    matchId = match.id ?: 0,
                    questionId = questionIdByMatch[match.id],
                    leagueName = match.league.name,
                    homeTeam = match.homeTeam,
                    awayTeam = match.awayTeam,
                    matchTime = match.matchTime.toString(),
                    status = match.matchStatus.name
                )
            }
        return ResponseEntity.ok(ApiEnvelope.ok(upcomingMatches))
    }

    /**
     * Match 연결 Question 목록 조회
     * GET /api/admin/sports/questions
     */
    @GetMapping("/questions")
    fun getMatchQuestions(): ResponseEntity<ApiEnvelope<List<MatchQuestionView>>> {
        val start = LocalDateTime.now(ZoneOffset.UTC)
        val end = start.plusDays(fetchWindowDays)
        val questions = questionRepository.findAllMatchQuestions()
            .filter { q ->
                val matchTime = q.match?.matchTime ?: return@filter false
                !matchTime.isBefore(start) && !matchTime.isAfter(end)
            }
        val views = questions.map { q ->
            MatchQuestionView(
                questionId = q.id!!,
                title = q.title,
                category = q.category,
                status = q.status.name,
                phase = q.phase?.name,
                matchId = q.match?.id,
                matchTime = q.match?.matchTime?.toString(),
                createdAt = q.createdAt.toString()
            )
        }
        return ResponseEntity.ok(ApiEnvelope.ok(views))
    }

    /**
     * Match → Question 수동 생성 트리거 (테스트용)
     * POST /api/admin/sports/generate-match-questions
     */
    @PostMapping("/generate-match-questions")
    fun generateMatchQuestions(): ResponseEntity<ApiEnvelope<MatchQuestionGenerateResult>> {
        val result = matchQuestionGeneratorService.generateQuestions()
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    private fun buildQuestionIdByMatch(): Map<Long, Long> {
        return questionRepository.findAllMatchQuestions().mapNotNull { question ->
            val matchId = question.match?.id
            val questionId = question.id
            if (matchId != null && questionId != null) {
                matchId to questionId
            } else {
                null
            }
        }.toMap()
    }

    private fun toLiveMatchInfo(match: Match, questionId: Long?): LiveMatchInfo {
        return LiveMatchInfo(
            matchId = match.id ?: 0,
            questionId = questionId,
            leagueName = match.league.name,
            homeTeam = match.homeTeam,
            awayTeam = match.awayTeam,
            homeScore = match.homeScore ?: 0,
            awayScore = match.awayScore ?: 0,
            matchDate = match.matchTime.toString(),
            status = match.matchStatus.name
        )
    }
}

data class LiveMatchInfo(
    val matchId: Long,
    val questionId: Long?,
    val leagueName: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val matchDate: String,
    val status: String
)

data class MatchQuestionView(
    val questionId: Long,
    val title: String,
    val category: String?,
    val status: String,
    val phase: String?,
    val matchId: Long?,
    val matchTime: String?,
    val createdAt: String
)

data class UpcomingMatchInfo(
    val matchId: Long,
    val questionId: Long?,
    val leagueName: String,
    val homeTeam: String,
    val awayTeam: String,
    val matchTime: String,
    val status: String
)
