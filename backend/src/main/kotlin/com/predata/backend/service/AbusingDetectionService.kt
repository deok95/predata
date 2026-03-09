package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.policy.AbusingDetectionPolicy
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
        AbusingDetectionPolicy.defaultCountries.forEach { country ->
            val gap = calculateGapByCountry(votes, bets, country)
            if (AbusingDetectionPolicy.shouldIncludeGap(gap.gapIndex)) {
                suspiciousGroups.add(gap)
            }
        }

        // 2. 직업별 괴리율 분석 (findIdsByJobCategory 사용)
        AbusingDetectionPolicy.defaultJobs.forEach { job ->
            val gap = calculateGapByJob(votes, bets, job)
            if (AbusingDetectionPolicy.shouldIncludeGap(gap.gapIndex)) {
                suspiciousGroups.add(gap)
            }
        }

        // 3. 연령대별 괴리율 분석 (findIdsByAgeGroupBetween 사용)
        AbusingDetectionPolicy.defaultAgeGroups.forEach { ageGroup ->
            val gap = calculateGapByAge(votes, bets, ageGroup)
            if (AbusingDetectionPolicy.shouldIncludeGap(gap.gapIndex)) {
                suspiciousGroups.add(gap)
            }
        }

        // 4. Latency 기반 봇 탐지 (이미 votes 로드됨)
        val fastClickers = detectFastClickers(votes)
        if (AbusingDetectionPolicy.shouldIncludeFastClickers(fastClickers.memberCount)) {
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
            recommendation = AbusingDetectionPolicy.recommendation(
                criticalCount = suspiciousGroups.count { it.riskLevel == RiskLevel.CRITICAL },
                highCount = suspiciousGroups.count { it.riskLevel == RiskLevel.HIGH },
                overallGap = overallGap
            )
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
            voteYesPercentage = AbusingDetectionPolicy.format2(voteYesPct),
            betYesPercentage = AbusingDetectionPolicy.format2(betYesPct),
            gapIndex = AbusingDetectionPolicy.format2(gap),
            memberCount = memberCount,
            riskLevel = AbusingDetectionPolicy.classifyGapRisk(gap)
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
            voteYesPercentage = AbusingDetectionPolicy.format2(voteYesPct),
            betYesPercentage = AbusingDetectionPolicy.format2(betYesPct),
            gapIndex = AbusingDetectionPolicy.format2(gap),
            memberCount = memberCount,
            riskLevel = AbusingDetectionPolicy.classifyGapRisk(gap)
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
            voteYesPercentage = AbusingDetectionPolicy.format2(voteYesPct),
            betYesPercentage = AbusingDetectionPolicy.format2(betYesPct),
            gapIndex = AbusingDetectionPolicy.format2(gap),
            memberCount = memberCount,
            riskLevel = AbusingDetectionPolicy.classifyGapRisk(gap)
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
        val fastClickers = votes.filter { AbusingDetectionPolicy.isFastClicker(it.latencyMs) }

        val voteYesPct = if (fastClickers.isNotEmpty()) {
            fastClickers.count { it.choice == Choice.YES } * 100.0 / fastClickers.size
        } else 0.0

        return SuspiciousGroup(
            criteria = "latency<1000ms",
            voteYesPercentage = AbusingDetectionPolicy.format2(voteYesPct),
            betYesPercentage = 0.0,
            gapIndex = 0.0,
            memberCount = fastClickers.size,
            riskLevel = AbusingDetectionPolicy.classifyFastClickerRisk(fastClickers.size)
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
