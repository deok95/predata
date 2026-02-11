package com.predata.backend.controller

import com.predata.backend.domain.SportsMatch
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.SportsMatchRepository
import com.predata.backend.service.GenerationResult
import com.predata.backend.service.QuestionAutoGenerationService
import com.predata.backend.service.SportsSettlementResult
import com.predata.backend.service.UpdateResult
import com.predata.backend.sports.service.MatchQuestionGenerateResult
import com.predata.backend.sports.service.MatchQuestionGeneratorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/sports")
@CrossOrigin(origins = ["http://localhost:3000"])
class SportsManagementController(
    private val questionAutoGenerationService: QuestionAutoGenerationService,
    private val sportsMatchRepository: SportsMatchRepository,
    private val matchQuestionGeneratorService: MatchQuestionGeneratorService,
    private val questionRepository: QuestionRepository
) {

    /**
     * 수동으로 스포츠 질문 생성 트리거
     * POST /api/admin/sports/generate
     */
    @PostMapping("/generate")
    fun manualGenerate(): ResponseEntity<GenerationResult> {
        val result = questionAutoGenerationService.generateSportsQuestions()
        return ResponseEntity.ok(result)
    }

    /**
     * 수동으로 경기 결과 업데이트
     * POST /api/admin/sports/update-results
     */
    @PostMapping("/update-results")
    fun manualUpdateResults(): ResponseEntity<UpdateResult> {
        val result = questionAutoGenerationService.updateFinishedMatches()
        return ResponseEntity.ok(result)
    }

    /**
     * 수동으로 자동 정산 실행
     * POST /api/admin/sports/settle
     */
    @PostMapping("/settle")
    fun manualSettle(): ResponseEntity<SportsSettlementResult> {
        val result = questionAutoGenerationService.autoSettleSportsQuestions()
        return ResponseEntity.ok(result)
    }

    /**
     * 현재 LIVE 경기 목록 조회
     * GET /api/admin/sports/live
     */
    @GetMapping("/live")
    fun getLiveMatches(): ResponseEntity<List<LiveMatchInfo>> {
        val liveMatches = sportsMatchRepository.findByStatus("LIVE")
        val liveMatchInfos = liveMatches.map { match ->
            LiveMatchInfo(
                matchId = match.id ?: 0,
                questionId = match.questionId,
                leagueName = match.leagueName,
                homeTeam = match.homeTeam,
                awayTeam = match.awayTeam,
                homeScore = match.homeScore ?: 0,
                awayScore = match.awayScore ?: 0,
                matchDate = match.matchDate.toString(),
                status = match.status
            )
        }
        return ResponseEntity.ok(liveMatchInfos)
    }

    /**
     * 예정된 경기 목록 조회
     * GET /api/admin/sports/upcoming
     */
    @GetMapping("/upcoming")
    fun getUpcomingMatches(): ResponseEntity<List<SportsMatch>> {
        val upcomingMatches = sportsMatchRepository.findByStatus("SCHEDULED")
        return ResponseEntity.ok(upcomingMatches)
    }

    /**
     * Match 연결 Question 목록 조회
     * GET /api/admin/sports/questions
     */
    @GetMapping("/questions")
    fun getMatchQuestions(): ResponseEntity<List<MatchQuestionView>> {
        val questions = questionRepository.findAllMatchQuestions()
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
        return ResponseEntity.ok(views)
    }

    /**
     * Match → Question 수동 생성 트리거 (테스트용)
     * POST /api/admin/sports/generate-match-questions
     */
    @PostMapping("/generate-match-questions")
    fun generateMatchQuestions(): ResponseEntity<MatchQuestionGenerateResult> {
        val result = matchQuestionGeneratorService.generateQuestions()
        return ResponseEntity.ok(result)
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
