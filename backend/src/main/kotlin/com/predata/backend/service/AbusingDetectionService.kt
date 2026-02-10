package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.dto.AbusingReport
import com.predata.backend.dto.RiskLevel
import com.predata.backend.dto.SuspiciousGroup
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Service
class AbusingDetectionService(
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val questionRepository: QuestionRepository
) {

    /**
     * 종합 어뷰징 분석
     * 최적화: 활동 데이터 1회 조회 후 메모리 필터링, findAll() 제거
     */
    @Transactional(readOnly = true)
    @Cacheable(value = ["abusingReport"], key = "#questionId")
    fun analyzeAbusingPatterns(questionId: Long): AbusingReport {
        // 활동 데이터 1회만 조회
        val allActivities = activityRepository.findByQuestionId(questionId)
        val votes = allActivities.filter { it.activityType == ActivityType.VOTE }
        val bets = allActivities.filter { it.activityType == ActivityType.BET }

        val suspiciousGroups = mutableListOf<SuspiciousGroup>()

        // 1. 국가별 괴리율 분석 (findIdsByCountryCode 사용)
        val countries = listOf("KR", "US", "JP", "SG", "VN")
        countries.forEach { country ->
            val gap = calculateGapByCountry(votes, bets, country)
            if (gap.gapIndex > 15.0) {
                suspiciousGroups.add(gap)
            }
        }

        // 2. 직업별 괴리율 분석 (findIdsByJobCategory 사용)
        val jobs = listOf("IT", "Finance", "Student", "Medical", "Service")
        jobs.forEach { job ->
            val gap = calculateGapByJob(votes, bets, job)
            if (gap.gapIndex > 15.0) {
                suspiciousGroups.add(gap)
            }
        }

        // 3. 연령대별 괴리율 분석 (findIdsByAgeGroupBetween 사용)
        listOf(20, 30, 40, 50).forEach { ageGroup ->
            val gap = calculateGapByAge(votes, bets, ageGroup)
            if (gap.gapIndex > 15.0) {
                suspiciousGroups.add(gap)
            }
        }

        // 4. Latency 기반 봇 탐지 (이미 votes 로드됨)
        val fastClickers = detectFastClickers(votes)
        if (fastClickers.memberCount > 500) {
            suspiciousGroups.add(fastClickers)
        }

        // 전체 괴리율 계산 (이미 로드된 데이터 사용)
        val overallGap = calculateOverallGap(votes, bets)

        // 의심 회원 수 계산 (findIds 메서드 사용)
        val suspiciousMemberIds = suspiciousGroups
            .flatMap { getMembersForCriteria(it.criteria) }
            .toSet()

        return AbusingReport(
            questionId = questionId,
            suspiciousGroups = suspiciousGroups.sortedByDescending { it.gapIndex },
            overallGap = overallGap,
            totalMembers = votes.map { it.memberId }.distinct().size,
            suspiciousMembers = suspiciousMemberIds.size,
            recommendation = generateRecommendation(suspiciousGroups, overallGap)
        )
    }

    /**
     * 국가별 괴리율 계산
     * 최적화: findIdsByCountryCode 사용
     */
    private fun calculateGapByCountry(
        votes: List<Activity>,
        bets: List<Activity>,
        countryCode: String
    ): SuspiciousGroup {
        val memberIds = memberRepository.findIdsByCountryCode(countryCode).toSet()
        val memberCount = memberRepository.countByCountryCode(countryCode)

        val countryVotes = votes.filter { it.memberId in memberIds }
        val countryBets = bets.filter { it.memberId in memberIds }

        val voteYesPct = if (countryVotes.isNotEmpty()) {
            countryVotes.count { it.choice == Choice.YES } * 100.0 / countryVotes.size
        } else 0.0

        val betYesPct = calculateBetYesPercentage(countryBets)
        val gap = abs(voteYesPct - betYesPct)

        return SuspiciousGroup(
            criteria = "country=$countryCode",
            voteYesPercentage = "%.2f".format(voteYesPct).toDouble(),
            betYesPercentage = "%.2f".format(betYesPct).toDouble(),
            gapIndex = "%.2f".format(gap).toDouble(),
            memberCount = memberCount,
            riskLevel = classifyRisk(gap)
        )
    }

    /**
     * 직업별 괴리율 계산
     * 최적화: findIdsByJobCategory 사용
     */
    private fun calculateGapByJob(
        votes: List<Activity>,
        bets: List<Activity>,
        jobCategory: String
    ): SuspiciousGroup {
        val memberIds = memberRepository.findIdsByJobCategory(jobCategory).toSet()
        val memberCount = memberRepository.countByJobCategory(jobCategory)

        val jobVotes = votes.filter { it.memberId in memberIds }
        val jobBets = bets.filter { it.memberId in memberIds }

        val voteYesPct = if (jobVotes.isNotEmpty()) {
            jobVotes.count { it.choice == Choice.YES } * 100.0 / jobVotes.size
        } else 0.0

        val betYesPct = calculateBetYesPercentage(jobBets)
        val gap = abs(voteYesPct - betYesPct)

        return SuspiciousGroup(
            criteria = "job=$jobCategory",
            voteYesPercentage = "%.2f".format(voteYesPct).toDouble(),
            betYesPercentage = "%.2f".format(betYesPct).toDouble(),
            gapIndex = "%.2f".format(gap).toDouble(),
            memberCount = memberCount,
            riskLevel = classifyRisk(gap)
        )
    }

    /**
     * 연령대별 괴리율 계산
     * 최적화: findIdsByAgeGroupBetween 사용
     */
    private fun calculateGapByAge(
        votes: List<Activity>,
        bets: List<Activity>,
        ageGroup: Int
    ): SuspiciousGroup {
        val memberIds = memberRepository.findIdsByAgeGroupBetween(ageGroup, ageGroup + 10).toSet()
        val memberCount = memberRepository.countByAgeGroupBetween(ageGroup, ageGroup + 10)

        val ageVotes = votes.filter { it.memberId in memberIds }
        val ageBets = bets.filter { it.memberId in memberIds }

        val voteYesPct = if (ageVotes.isNotEmpty()) {
            ageVotes.count { it.choice == Choice.YES } * 100.0 / ageVotes.size
        } else 0.0

        val betYesPct = calculateBetYesPercentage(ageBets)
        val gap = abs(voteYesPct - betYesPct)

        return SuspiciousGroup(
            criteria = "age=${ageGroup}s",
            voteYesPercentage = "%.2f".format(voteYesPct).toDouble(),
            betYesPercentage = "%.2f".format(betYesPct).toDouble(),
            gapIndex = "%.2f".format(gap).toDouble(),
            memberCount = memberCount,
            riskLevel = classifyRisk(gap)
        )
    }

    /**
     * 베팅 YES 비율 계산 (공통 로직)
     */
    private fun calculateBetYesPercentage(bets: List<Activity>): Double {
        if (bets.isEmpty()) return 0.0
        val totalBetAmount = bets.sumOf { it.amount }
        return if (totalBetAmount > 0) {
            bets.filter { it.choice == Choice.YES }.sumOf { it.amount } * 100.0 / totalBetAmount
        } else 0.0
    }

    /**
     * 빠른 클릭 탐지 (봇 의심)
     */
    private fun detectFastClickers(votes: List<Activity>): SuspiciousGroup {
        val fastClickers = votes.filter {
            it.latencyMs != null && it.latencyMs < 1000
        }

        val voteYesPct = if (fastClickers.isNotEmpty()) {
            fastClickers.count { it.choice == Choice.YES } * 100.0 / fastClickers.size
        } else 0.0

        return SuspiciousGroup(
            criteria = "latency<1000ms",
            voteYesPercentage = "%.2f".format(voteYesPct).toDouble(),
            betYesPercentage = 0.0,
            gapIndex = 0.0,
            memberCount = fastClickers.size,
            riskLevel = when {
                fastClickers.size > 2000 -> RiskLevel.CRITICAL
                fastClickers.size > 1000 -> RiskLevel.HIGH
                else -> RiskLevel.MEDIUM
            }
        )
    }

    /**
     * 전체 괴리율 계산
     */
    private fun calculateOverallGap(votes: List<Activity>, bets: List<Activity>): Double {
        if (votes.isEmpty() || bets.isEmpty()) return 0.0

        val voteYesPct = votes.count { it.choice == Choice.YES } * 100.0 / votes.size
        val betYesPct = calculateBetYesPercentage(bets)

        return abs(voteYesPct - betYesPct)
    }

    /**
     * 위험도 분류
     */
    private fun classifyRisk(gap: Double): RiskLevel {
        return when {
            gap > 50 -> RiskLevel.CRITICAL
            gap > 30 -> RiskLevel.HIGH
            gap > 15 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    /**
     * 권장 사항 생성
     */
    private fun generateRecommendation(
        groups: List<SuspiciousGroup>,
        overallGap: Double
    ): String {
        val criticalCount = groups.count { it.riskLevel == RiskLevel.CRITICAL }
        val highCount = groups.count { it.riskLevel == RiskLevel.HIGH }

        return when {
            criticalCount > 0 ->
                "⚠️ CRITICAL: ${criticalCount}개 그룹에서 극단적 괴리 발견. 해당 그룹 데이터 제외 권장."
            highCount > 2 ->
                "⚠️ HIGH: ${highCount}개 그룹에서 높은 괴리 발견. 필터링 후 사용 권장."
            overallGap > 20 ->
                "⚠️ 전체 괴리율 ${"%.1f".format(overallGap)}%. Latency 필터링 권장."
            overallGap < 10 ->
                "✅ 전체 괴리율 ${"%.1f".format(overallGap)}%. 데이터 품질 양호."
            else ->
                "✅ 전체 괴리율 ${"%.1f".format(overallGap)}%. 사용 가능한 수준."
        }
    }

    /**
     * 조건에 해당하는 회원 ID 추출
     * 최적화: findIds 메서드 사용
     */
    private fun getMembersForCriteria(criteria: String): List<Long> {
        val parts = criteria.split("=")
        if (parts.size != 2) return emptyList()
        val (key, value) = parts

        return when (key) {
            "country" -> memberRepository.findIdsByCountryCode(value)
            "job" -> memberRepository.findIdsByJobCategory(value)
            "age" -> {
                val age = value.replace("s", "").toIntOrNull() ?: return emptyList()
                memberRepository.findIdsByAgeGroupBetween(age, age + 10)
            }
            else -> emptyList()
        }
    }
}
