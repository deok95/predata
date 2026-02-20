package com.predata.backend.controller

import com.predata.backend.domain.Choice
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.*
import com.predata.backend.exception.ForbiddenException
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.AbusingDetectionService
import com.predata.backend.service.DataQualityService
import com.predata.backend.service.PersonaWeightService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analysis")
class DataAnalysisController(
    private val abusingDetectionService: AbusingDetectionService,
    private val dataQualityService: DataQualityService,
    private val personaWeightService: PersonaWeightService,
    private val questionRepository: QuestionRepository
) {

    /**
     * 투표 결과 공개 가능 여부 확인
     * - BETTING_OPEN 이후부터만 분석 데이터 공개
     */
    private fun checkPhaseForAnalysis(questionId: Long) {
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("질문을 찾을 수 없습니다.")
        }

        if (question.votingPhase.ordinal < VotingPhase.BETTING_OPEN.ordinal) {
            throw ForbiddenException("Vote results not yet revealed.")
        }
    }

    /**
     * 어뷰징 분석 리포트
     * GET /api/analysis/questions/{id}/abusing-report
     */
    @GetMapping("/questions/{id}/abusing-report")
    fun getAbusingReport(@PathVariable id: Long): ResponseEntity<ApiEnvelope<AbusingReport>> {
        checkPhaseForAnalysis(id)
        val report = abusingDetectionService.analyzeAbusingPatterns(id)
        return ResponseEntity.ok(ApiEnvelope.ok(report))
    }

    /**
     * 품질 점수 조회
     * GET /api/analysis/questions/{id}/quality-score
     */
    @GetMapping("/questions/{id}/quality-score")
    fun getQualityScore(@PathVariable id: Long): ResponseEntity<ApiEnvelope<QualityScoreResponse>> {
        checkPhaseForAnalysis(id)
        val score = dataQualityService.calculateQualityScore(id)
        val grade = when {
            score >= 90 -> "A"
            score >= 80 -> "B"
            score >= 70 -> "C"
            score >= 60 -> "D"
            else -> "F"
        }
        return ResponseEntity.ok(ApiEnvelope.ok(QualityScoreResponse(questionId = id, qualityScore = score, grade = grade)))
    }

    /**
     * 가중치 적용 투표 결과
     * GET /api/analysis/questions/{id}/weighted-votes
     */
    @GetMapping("/questions/{id}/weighted-votes")
    fun getWeightedVotes(
        @PathVariable id: Long,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<ApiEnvelope<WeightedVoteResult>> {
        checkPhaseForAnalysis(id)
        val result = personaWeightService.calculateWeightedVotes(id, category)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 국가별 투표 분석
     * GET /api/analysis/questions/{id}/by-country
     */
    @GetMapping("/questions/{id}/by-country")
    fun getVotesByCountry(@PathVariable id: Long): ResponseEntity<ApiEnvelope<Any>> {
        checkPhaseForAnalysis(id)
        val result = personaWeightService.calculateVotesByCountry(id)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * 프리미엄 데이터 (필터링 + 가중치)
     * GET /api/analysis/questions/{id}/premium-data
     */
    @GetMapping("/questions/{id}/premium-data")
    fun getPremiumData(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "2000") minLatencyMs: Int,
        @RequestParam(defaultValue = "false") onlyBettors: Boolean
    ): ResponseEntity<ApiEnvelope<PremiumDataResponse>> {
        checkPhaseForAnalysis(id)

        val allVotes = dataQualityService.applyFilters(id, FilterOptions(minLatencyMs = 0, onlyBettors = false))
        val cleanedVotes = dataQualityService.applyFilters(id, FilterOptions(minLatencyMs = minLatencyMs, onlyBettors = onlyBettors))
        val weightedResult = personaWeightService.calculateWeightedVotes(id)
        val qualityScore = dataQualityService.calculateQualityScore(id)
        val abusingReport = abusingDetectionService.analyzeAbusingPatterns(id)
        val originalGap = abusingReport.overallGap

        val gapReduction = originalGap - kotlin.math.abs(
            weightedResult.weightedYesPercentage -
            (cleanedVotes.count { it.choice == Choice.YES } * 100.0 / cleanedVotes.size)
        )

        return ResponseEntity.ok(
            ApiEnvelope.ok(
                PremiumDataResponse(
                    questionId = id,
                    rawVoteCount = allVotes.size,
                    cleanedVoteCount = cleanedVotes.size,
                    weightedResult = weightedResult,
                    qualityScore = qualityScore,
                    gapReduction = String.format("%.2f", gapReduction).toDouble()
                )
            )
        )
    }

    /**
     * 빠른 클릭 탐지 (봇 의심)
     * GET /api/analysis/questions/{id}/fast-clickers
     */
    @GetMapping("/questions/{id}/fast-clickers")
    fun getFastClickers(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "1000") thresholdMs: Int
    ): ResponseEntity<ApiEnvelope<FastClickersResponse>> {
        checkPhaseForAnalysis(id)
        val fastClickers = dataQualityService.detectFastClickers(id, thresholdMs)
        val totalVotes = dataQualityService.applyFilters(id, FilterOptions(minLatencyMs = 0)).size
        val percentage = String.format("%.2f", fastClickers.size * 100.0 / totalVotes).toDouble()

        return ResponseEntity.ok(
            ApiEnvelope.ok(FastClickersResponse(questionId = id, thresholdMs = thresholdMs, suspiciousCount = fastClickers.size, percentage = percentage))
        )
    }

    /**
     * 필터링 시뮬레이션
     * POST /api/analysis/questions/{id}/simulate-filter
     */
    @PostMapping("/questions/{id}/simulate-filter")
    fun simulateFilter(
        @PathVariable id: Long,
        @RequestBody options: FilterOptions
    ): ResponseEntity<ApiEnvelope<FilterSimulationResponse>> {
        checkPhaseForAnalysis(id)
        val allVotes = dataQualityService.applyFilters(id, FilterOptions(minLatencyMs = 0))
        val filteredVotes = dataQualityService.applyFilters(id, options)

        val originalYesPct = allVotes.count { it.choice == Choice.YES } * 100.0 / allVotes.size
        val filteredYesPct = filteredVotes.count { it.choice == Choice.YES } * 100.0 / filteredVotes.size

        return ResponseEntity.ok(
            ApiEnvelope.ok(
                FilterSimulationResponse(
                    questionId = id,
                    filterOptions = options,
                    originalVoteCount = allVotes.size,
                    filteredVoteCount = filteredVotes.size,
                    removedCount = allVotes.size - filteredVotes.size,
                    originalYesPercentage = String.format("%.2f", originalYesPct).toDouble(),
                    filteredYesPercentage = String.format("%.2f", filteredYesPct).toDouble(),
                    percentageChange = String.format("%.2f", filteredYesPct - originalYesPct).toDouble()
                )
            )
        )
    }
}
