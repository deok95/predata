package com.predata.sports.controller

import com.predata.sports.domain.SportsMatch
import com.predata.sports.repository.SportsMatchRepository
import com.predata.sports.service.GenerationResult
import com.predata.sports.service.QuestionAutoGenerationService
import com.predata.sports.service.SportsSettlementResult
import com.predata.sports.service.UpdateResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/sports")
@CrossOrigin(origins = ["http://localhost:3000"])
class SportsManagementController(
    private val questionAutoGenerationService: QuestionAutoGenerationService,
    private val sportsMatchRepository: SportsMatchRepository
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
