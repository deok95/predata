package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.dto.AbusingReport
import com.predata.backend.dto.RiskLevel
import com.predata.backend.dto.SuspiciousGroup
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
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
     */
    @Transactional(readOnly = true)
    fun analyzeAbusingPatterns(questionId: Long): AbusingReport {
        val suspiciousGroups = mutableListOf<SuspiciousGroup>()
        
        // 1. 국가별 괴리율 분석
        val countries = listOf("KR", "US", "JP", "SG", "VN")
        countries.forEach { country ->
            val gap = calculateGapByCountry(questionId, country)
            if (gap.gapIndex > 15.0) { // 15% 이상 괴리면 추가
                suspiciousGroups.add(gap)
            }
        }
        
        // 2. 직업별 괴리율 분석
        val jobs = listOf("IT", "Finance", "Student", "Medical", "Service")
        jobs.forEach { job ->
            val gap = calculateGapByJob(questionId, job)
            if (gap.gapIndex > 15.0) {
                suspiciousGroups.add(gap)
            }
        }
        
        // 3. 연령대별 괴리율 분석
        listOf(20, 30, 40, 50).forEach { ageGroup ->
            val gap = calculateGapByAge(questionId, ageGroup)
            if (gap.gapIndex > 15.0) {
                suspiciousGroups.add(gap)
            }
        }
        
        // 4. Latency 기반 봇 탐지
        val fastClickers = detectFastClickers(questionId)
        if (fastClickers.memberCount > 500) { // 500명 이상이면 의심
            suspiciousGroups.add(fastClickers)
        }
        
        // 전체 괴리율 계산
        val overallGap = calculateOverallGap(questionId)
        
        // 전체 회원 vs 의심 회원
        val allVotes = activityRepository.findByQuestionIdAndActivityType(
            questionId, ActivityType.VOTE
        )
        val suspiciousMemberIds = suspiciousGroups
            .flatMap { getMembersForCriteria(it.criteria) }
            .toSet()
        
        return AbusingReport(
            questionId = questionId,
            suspiciousGroups = suspiciousGroups.sortedByDescending { it.gapIndex },
            overallGap = overallGap,
            totalMembers = allVotes.map { it.memberId }.distinct().size,
            suspiciousMembers = suspiciousMemberIds.size,
            recommendation = generateRecommendation(suspiciousGroups, overallGap)
        )
    }

    /**
     * 국가별 괴리율 계산
     */
    private fun calculateGapByCountry(questionId: Long, countryCode: String): SuspiciousGroup {
        val members = memberRepository.findAll()
            .filter { it.countryCode == countryCode }
        val memberIds = members.map { it.id!! }.toSet()
        
        val votes = activityRepository.findByQuestionId(questionId)
            .filter { it.memberId in memberIds && it.activityType == ActivityType.VOTE }
        
        val bets = activityRepository.findByQuestionId(questionId)
            .filter { it.memberId in memberIds && it.activityType == ActivityType.BET }
        
        val voteYesPct = if (votes.isNotEmpty()) {
            votes.count { it.choice == Choice.YES } * 100.0 / votes.size
        } else 0.0
        
        val betYesPct = if (bets.isNotEmpty()) {
            val totalBetAmount = bets.sumOf { it.amount }
            if (totalBetAmount > 0) {
                bets.filter { it.choice == Choice.YES }.sumOf { it.amount } * 100.0 / totalBetAmount
            } else 0.0
        } else 0.0
        
        val gap = abs(voteYesPct - betYesPct)
        
        return SuspiciousGroup(
            criteria = "country=$countryCode",
            voteYesPercentage = String.format("%.2f", voteYesPct).toDouble(),
            betYesPercentage = String.format("%.2f", betYesPct).toDouble(),
            gapIndex = String.format("%.2f", gap).toDouble(),
            memberCount = members.size,
            riskLevel = classifyRisk(gap)
        )
    }

    /**
     * 직업별 괴리율 계산
     */
    private fun calculateGapByJob(questionId: Long, jobCategory: String): SuspiciousGroup {
        val members = memberRepository.findAll()
            .filter { it.jobCategory == jobCategory }
        val memberIds = members.map { it.id!! }.toSet()
        
        val votes = activityRepository.findByQuestionId(questionId)
            .filter { it.memberId in memberIds && it.activityType == ActivityType.VOTE }
        
        val bets = activityRepository.findByQuestionId(questionId)
            .filter { it.memberId in memberIds && it.activityType == ActivityType.BET }
        
        val voteYesPct = if (votes.isNotEmpty()) {
            votes.count { it.choice == Choice.YES } * 100.0 / votes.size
        } else 0.0
        
        val betYesPct = if (bets.isNotEmpty()) {
            val totalBetAmount = bets.sumOf { it.amount }
            if (totalBetAmount > 0) {
                bets.filter { it.choice == Choice.YES }.sumOf { it.amount } * 100.0 / totalBetAmount
            } else 0.0
        } else 0.0
        
        val gap = abs(voteYesPct - betYesPct)
        
        return SuspiciousGroup(
            criteria = "job=$jobCategory",
            voteYesPercentage = String.format("%.2f", voteYesPct).toDouble(),
            betYesPercentage = String.format("%.2f", betYesPct).toDouble(),
            gapIndex = String.format("%.2f", gap).toDouble(),
            memberCount = members.size,
            riskLevel = classifyRisk(gap)
        )
    }

    /**
     * 연령대별 괴리율 계산
     */
    private fun calculateGapByAge(questionId: Long, ageGroup: Int): SuspiciousGroup {
        val members = memberRepository.findAll()
            .filter { it.ageGroup != null && it.ageGroup in ageGroup until (ageGroup + 10) }
        val memberIds = members.map { it.id!! }.toSet()
        
        val votes = activityRepository.findByQuestionId(questionId)
            .filter { it.memberId in memberIds && it.activityType == ActivityType.VOTE }
        
        val bets = activityRepository.findByQuestionId(questionId)
            .filter { it.memberId in memberIds && it.activityType == ActivityType.BET }
        
        val voteYesPct = if (votes.isNotEmpty()) {
            votes.count { it.choice == Choice.YES } * 100.0 / votes.size
        } else 0.0
        
        val betYesPct = if (bets.isNotEmpty()) {
            val totalBetAmount = bets.sumOf { it.amount }
            if (totalBetAmount > 0) {
                bets.filter { it.choice == Choice.YES }.sumOf { it.amount } * 100.0 / totalBetAmount
            } else 0.0
        } else 0.0
        
        val gap = abs(voteYesPct - betYesPct)
        
        return SuspiciousGroup(
            criteria = "age=${ageGroup}s",
            voteYesPercentage = String.format("%.2f", voteYesPct).toDouble(),
            betYesPercentage = String.format("%.2f", betYesPct).toDouble(),
            gapIndex = String.format("%.2f", gap).toDouble(),
            memberCount = members.size,
            riskLevel = classifyRisk(gap)
        )
    }

    /**
     * 빠른 클릭 탐지 (봇 의심)
     */
    private fun detectFastClickers(questionId: Long): SuspiciousGroup {
        val votes = activityRepository.findByQuestionIdAndActivityType(
            questionId, 
            ActivityType.VOTE
        )
        
        val fastClickers = votes.filter { 
            it.latencyMs != null && it.latencyMs < 1000 
        }
        
        val voteYesPct = if (fastClickers.isNotEmpty()) {
            fastClickers.count { it.choice == Choice.YES } * 100.0 / fastClickers.size
        } else 0.0
        
        return SuspiciousGroup(
            criteria = "latency<1000ms",
            voteYesPercentage = String.format("%.2f", voteYesPct).toDouble(),
            betYesPercentage = 0.0,
            gapIndex = 0.0,
            memberCount = fastClickers.size,
            riskLevel = if (fastClickers.size > 2000) RiskLevel.CRITICAL 
                       else if (fastClickers.size > 1000) RiskLevel.HIGH
                       else RiskLevel.MEDIUM
        )
    }

    /**
     * 전체 괴리율 계산
     */
    private fun calculateOverallGap(questionId: Long): Double {
        val votes = activityRepository.findByQuestionIdAndActivityType(
            questionId, ActivityType.VOTE
        )
        val bets = activityRepository.findByQuestionIdAndActivityType(
            questionId, ActivityType.BET
        )
        
        if (votes.isEmpty() || bets.isEmpty()) return 0.0
        
        val voteYesPct = votes.count { it.choice == Choice.YES } * 100.0 / votes.size
        
        val totalBetAmount = bets.sumOf { it.amount }
        val betYesPct = if (totalBetAmount > 0) {
            bets.filter { it.choice == Choice.YES }.sumOf { it.amount } * 100.0 / totalBetAmount
        } else 0.0
        
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
                "⚠️ 전체 괴리율 ${String.format("%.1f", overallGap)}%. Latency 필터링 권장."
            overallGap < 10 ->
                "✅ 전체 괴리율 ${String.format("%.1f", overallGap)}%. 데이터 품질 양호."
            else -> 
                "✅ 전체 괴리율 ${String.format("%.1f", overallGap)}%. 사용 가능한 수준."
        }
    }

    /**
     * 조건에 해당하는 회원 ID 추출
     */
    private fun getMembersForCriteria(criteria: String): List<Long> {
        val (key, value) = criteria.split("=")
        
        return when (key) {
            "country" -> memberRepository.findAll()
                .filter { it.countryCode == value }
                .mapNotNull { it.id }
            "job" -> memberRepository.findAll()
                .filter { it.jobCategory == value }
                .mapNotNull { it.id }
            "age" -> {
                val age = value.replace("s", "").toIntOrNull() ?: return emptyList()
                memberRepository.findAll()
                    .filter { it.ageGroup != null && it.ageGroup in age until (age + 10) }
                    .mapNotNull { it.id }
            }
            else -> emptyList()
        }
    }
}
