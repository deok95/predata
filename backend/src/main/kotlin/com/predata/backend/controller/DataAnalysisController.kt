package com.predata.backend.controller

import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.*
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.AbusingDetectionService
import com.predata.backend.service.DataQualityService
import com.predata.backend.service.PersonaWeightService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(originPatterns = ["http://localhost:*", "http://127.0.0.1:*", "https://predata.io", "https://www.predata.io", "https://*.vercel.app", "https://*.trycloudflare.com"])
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
            throw IllegalStateException("Vote results not yet revealed.")
        }
    }

    /**
     * 어뷰징 분석 리포트
     * GET /api/analysis/questions/{id}/abusing-report
     */
    @GetMapping("/questions/{id}/abusing-report")
    fun getAbusingReport(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            checkPhaseForAnalysis(id)
            val report = abusingDetectionService.analyzeAbusingPatterns(id)
            ResponseEntity.ok(report)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
    }

    /**
     * 품질 점수 조회
     * GET /api/analysis/questions/{id}/quality-score
     */
    @GetMapping("/questions/{id}/quality-score")
    fun getQualityScore(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        return try {
            checkPhaseForAnalysis(id)
            val score = dataQualityService.calculateQualityScore(id)
            ResponseEntity.ok(
                mapOf(
                    "questionId" to id,
                    "qualityScore" to score,
                    "grade" to when {
                        score >= 90 -> "A"
                        score >= 80 -> "B"
                        score >= 70 -> "C"
                        score >= 60 -> "D"
                        else -> "F"
                    }
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
    }

    /**
     * 가중치 적용 투표 결과
     * GET /api/analysis/questions/{id}/weighted-votes
     */
    @GetMapping("/questions/{id}/weighted-votes")
    fun getWeightedVotes(
        @PathVariable id: Long,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<*> {
        return try {
            checkPhaseForAnalysis(id)
            val result = personaWeightService.calculateWeightedVotes(id, category)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
    }

    /**
     * 국가별 투표 분석
     * GET /api/analysis/questions/{id}/by-country
     */
    @GetMapping("/questions/{id}/by-country")
    fun getVotesByCountry(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            checkPhaseForAnalysis(id)
            val result = personaWeightService.calculateVotesByCountry(id)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
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
    ): ResponseEntity<*> {
        return try {
            checkPhaseForAnalysis(id)

            // 원본 데이터
            val allVotes = dataQualityService.applyFilters(
                id,
                FilterOptions(minLatencyMs = 0, onlyBettors = false)
            )

            // 필터링된 데이터
            val cleanedVotes = dataQualityService.applyFilters(
                id,
                FilterOptions(
                    minLatencyMs = minLatencyMs,
                    onlyBettors = onlyBettors
                )
            )

            // 가중치 적용
            val weightedResult = personaWeightService.calculateWeightedVotes(id)

            // 품질 점수
            val qualityScore = dataQualityService.calculateQualityScore(id)

            // 괴리율 감소 계산
            val abusingReport = abusingDetectionService.analyzeAbusingPatterns(id)
            val originalGap = abusingReport.overallGap

            // 필터링 후 재계산 (간단히 가중치 결과로 대체)
            val gapReduction = originalGap - kotlin.math.abs(
                weightedResult.weightedYesPercentage -
                (cleanedVotes.count { it.choice == com.predata.backend.domain.Choice.YES } * 100.0 / cleanedVotes.size)
            )

            ResponseEntity.ok(
                PremiumDataResponse(
                    questionId = id,
                    rawVoteCount = allVotes.size,
                    cleanedVoteCount = cleanedVotes.size,
                    weightedResult = weightedResult,
                    qualityScore = qualityScore,
                    gapReduction = String.format("%.2f", gapReduction).toDouble()
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
    }

    /**
     * 빠른 클릭 탐지 (봇 의심)
     * GET /api/analysis/questions/{id}/fast-clickers
     */
    @GetMapping("/questions/{id}/fast-clickers")
    fun getFastClickers(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "1000") thresholdMs: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            checkPhaseForAnalysis(id)
            val fastClickers = dataQualityService.detectFastClickers(id, thresholdMs)

            ResponseEntity.ok(
                mapOf(
                    "questionId" to id,
                    "thresholdMs" to thresholdMs,
                    "suspiciousCount" to fastClickers.size,
                    "percentage" to String.format(
                        "%.2f",
                        fastClickers.size * 100.0 / dataQualityService.applyFilters(
                            id,
                            FilterOptions(minLatencyMs = 0)
                        ).size
                    ).toDouble()
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
    }

    /**
     * 필터링 시뮬레이션
     * POST /api/analysis/questions/{id}/simulate-filter
     */
    @PostMapping("/questions/{id}/simulate-filter")
    fun simulateFilter(
        @PathVariable id: Long,
        @RequestBody options: FilterOptions
    ): ResponseEntity<Map<String, Any>> {
        return try {
            checkPhaseForAnalysis(id)
            val allVotes = dataQualityService.applyFilters(
                id,
                FilterOptions(minLatencyMs = 0)
            )

            val filteredVotes = dataQualityService.applyFilters(id, options)

            val originalYesPct = allVotes.count {
                it.choice == com.predata.backend.domain.Choice.YES
            } * 100.0 / allVotes.size

            val filteredYesPct = filteredVotes.count {
                it.choice == com.predata.backend.domain.Choice.YES
            } * 100.0 / filteredVotes.size

            ResponseEntity.ok(
                mapOf(
                    "questionId" to id,
                    "filterOptions" to options,
                    "originalVoteCount" to allVotes.size,
                    "filteredVoteCount" to filteredVotes.size,
                    "removedCount" to (allVotes.size - filteredVotes.size),
                    "originalYesPercentage" to String.format("%.2f", originalYesPct).toDouble(),
                    "filteredYesPercentage" to String.format("%.2f", filteredYesPct).toDouble(),
                    "percentageChange" to String.format("%.2f", filteredYesPct - originalYesPct).toDouble()
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("success" to false, "message" to (e.message ?: "Vote results not yet revealed.")))
        }
    }
}
