package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class AnalyticsService(
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val questionRepository: QuestionRepository
) {

    /**
     * 페르소나별 투표 분포 분석
     */
    @Transactional(readOnly = true)
    fun getVoteDemographics(questionId: Long): VoteDemographicsReport {
        val votes = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
        
        // 국가별 분포
        val byCountry = mutableMapOf<String, CountryVoteData>()
        // 직업별 분포
        val byJob = mutableMapOf<String, JobVoteData>()
        // 연령대별 분포
        val byAge = mutableMapOf<Int, AgeVoteData>()
        
        votes.forEach { vote ->
            val member = memberRepository.findById(vote.memberId).orElse(null) ?: return@forEach
            
            // 국가별 집계
            val countryData = byCountry.getOrPut(member.countryCode) {
                CountryVoteData(member.countryCode, 0, 0)
            }
            if (vote.choice == Choice.YES) countryData.yesCount++ else countryData.noCount++
            
            // 직업별 집계
            member.jobCategory?.let { job ->
                val jobData = byJob.getOrPut(job) {
                    JobVoteData(job, 0, 0)
                }
                if (vote.choice == Choice.YES) jobData.yesCount++ else jobData.noCount++
            }
            
            // 연령대별 집계
            member.ageGroup?.let { age ->
                val ageData = byAge.getOrPut(age) {
                    AgeVoteData(age, 0, 0)
                }
                if (vote.choice == Choice.YES) ageData.yesCount++ else ageData.noCount++
            }
        }
        
        return VoteDemographicsReport(
            questionId = questionId,
            totalVotes = votes.size,
            byCountry = byCountry.values.toList(),
            byJob = byJob.values.toList(),
            byAge = byAge.values.sortedBy { it.ageGroup }.toList()
        )
    }
    
    /**
     * 투표 vs 베팅 괴리율 분석
     */
    @Transactional(readOnly = true)
    fun getVoteBetGapAnalysis(questionId: Long): VoteBetGapReport {
        val votes = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
        val bets = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.BET)
        
        // 투표 분포
        val voteYesCount = votes.count { it.choice == Choice.YES }
        val voteNoCount = votes.size - voteYesCount
        val voteYesPercentage = if (votes.isNotEmpty()) {
            (voteYesCount.toDouble() / votes.size * 100)
        } else 0.0
        
        // 베팅 분포 (금액 기준)
        val betYesAmount = bets.filter { it.choice == Choice.YES }.sumOf { it.amount }
        val betNoAmount = bets.filter { it.choice == Choice.NO }.sumOf { it.amount }
        val totalBetAmount = betYesAmount + betNoAmount
        val betYesPercentage = if (totalBetAmount > 0) {
            (betYesAmount.toDouble() / totalBetAmount * 100)
        } else 0.0
        
        // 괴리율 계산
        val gapPercentage = Math.abs(voteYesPercentage - betYesPercentage)
        
        return VoteBetGapReport(
            questionId = questionId,
            voteDistribution = DistributionData(
                yesCount = voteYesCount,
                noCount = voteNoCount,
                yesPercentage = voteYesPercentage,
                noPercentage = 100 - voteYesPercentage
            ),
            betDistribution = DistributionData(
                yesCount = bets.count { it.choice == Choice.YES },
                noCount = bets.count { it.choice == Choice.NO },
                yesPercentage = betYesPercentage,
                noPercentage = 100 - betYesPercentage
            ),
            gapPercentage = gapPercentage,
            qualityScore = calculateQualityScore(gapPercentage)
        )
    }
    
    /**
     * 어뷰징 필터링 효과 분석
     */
    @Transactional(readOnly = true)
    fun getFilteringEffectReport(questionId: Long): FilteringEffectReport {
        val allVotes = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
        
        // 필터링 기준 적용
        val latencyThreshold = 2000 // 2초 이하는 의심
        val suspiciousVotes = allVotes.filter { vote ->
            (vote.latencyMs ?: Int.MAX_VALUE) < latencyThreshold
        }
        
        val cleanVotes = allVotes - suspiciousVotes.toSet()
        
        // 필터링 전 분포
        val beforeYes = allVotes.count { it.choice == Choice.YES }
        val beforeTotal = allVotes.size
        val beforeYesPercentage = if (beforeTotal > 0) {
            (beforeYes.toDouble() / beforeTotal * 100)
        } else 0.0
        
        // 필터링 후 분포
        val afterYes = cleanVotes.count { it.choice == Choice.YES }
        val afterTotal = cleanVotes.size
        val afterYesPercentage = if (afterTotal > 0) {
            (afterYes.toDouble() / afterTotal * 100)
        } else 0.0
        
        return FilteringEffectReport(
            questionId = questionId,
            beforeFiltering = FilteringData(
                totalCount = beforeTotal,
                yesPercentage = beforeYesPercentage,
                noPercentage = 100 - beforeYesPercentage
            ),
            afterFiltering = FilteringData(
                totalCount = afterTotal,
                yesPercentage = afterYesPercentage,
                noPercentage = 100 - afterYesPercentage
            ),
            filteredCount = suspiciousVotes.size,
            filteredPercentage = if (beforeTotal > 0) {
                (suspiciousVotes.size.toDouble() / beforeTotal * 100)
            } else 0.0
        )
    }
    
    /**
     * 전체 데이터 품질 대시보드
     */
    @Transactional(readOnly = true)
    fun getQualityDashboard(questionId: Long): QualityDashboard {
        val demographics = getVoteDemographics(questionId)
        val gapAnalysis = getVoteBetGapAnalysis(questionId)
        val filteringEffect = getFilteringEffectReport(questionId)
        
        return QualityDashboard(
            questionId = questionId,
            demographics = demographics,
            gapAnalysis = gapAnalysis,
            filteringEffect = filteringEffect,
            overallQualityScore = gapAnalysis.qualityScore
        )
    }
    
    /**
     * 품질 점수 계산 (괴리율 기반)
     * 괴리율이 낮을수록 높은 점수
     */
    private fun calculateQualityScore(gapPercentage: Double): Double {
        return when {
            gapPercentage < 5.0 -> 95.0 + (5.0 - gapPercentage) // 95-100점
            gapPercentage < 10.0 -> 85.0 + (10.0 - gapPercentage) // 85-95점
            gapPercentage < 20.0 -> 70.0 + (20.0 - gapPercentage) * 0.75 // 70-85점
            else -> Math.max(0.0, 70.0 - (gapPercentage - 20.0)) // 0-70점
        }
    }

    /**
     * 글로벌 통계 조회
     */
    @Transactional(readOnly = true)
    fun getGlobalStats(): Map<String, Any> {
        val totalQuestions = questionRepository.count()
        val totalMembers = memberRepository.count()
        val totalActivities = activityRepository.count()

        // 총 베팅 풀 계산
        val questions = questionRepository.findAll()
        val totalBetPool = questions.sumOf { it.totalBetPool }

        return mapOf(
            "totalPredictions" to totalActivities,
            "totalValueLocked" to totalBetPool,
            "totalRewards" to (totalBetPool * 0.95).toLong(), // 예시: TVL의 95%가 보상
            "activeUsers" to totalMembers,
            "totalQuestions" to totalQuestions
        )
    }
}

// ===== DTOs =====

data class VoteDemographicsReport(
    val questionId: Long,
    val totalVotes: Int,
    val byCountry: List<CountryVoteData>,
    val byJob: List<JobVoteData>,
    val byAge: List<AgeVoteData>
)

data class CountryVoteData(
    val countryCode: String,
    var yesCount: Int,
    var noCount: Int
) {
    val total: Int get() = yesCount + noCount
    val yesPercentage: Double get() = if (total > 0) (yesCount.toDouble() / total * 100) else 0.0
}

data class JobVoteData(
    val jobCategory: String,
    var yesCount: Int,
    var noCount: Int
) {
    val total: Int get() = yesCount + noCount
    val yesPercentage: Double get() = if (total > 0) (yesCount.toDouble() / total * 100) else 0.0
}

data class AgeVoteData(
    val ageGroup: Int,
    var yesCount: Int,
    var noCount: Int
) {
    val total: Int get() = yesCount + noCount
    val yesPercentage: Double get() = if (total > 0) (yesCount.toDouble() / total * 100) else 0.0
}

data class VoteBetGapReport(
    val questionId: Long,
    val voteDistribution: DistributionData,
    val betDistribution: DistributionData,
    val gapPercentage: Double,
    val qualityScore: Double
)

data class DistributionData(
    val yesCount: Int,
    val noCount: Int,
    val yesPercentage: Double,
    val noPercentage: Double
)

data class FilteringEffectReport(
    val questionId: Long,
    val beforeFiltering: FilteringData,
    val afterFiltering: FilteringData,
    val filteredCount: Int,
    val filteredPercentage: Double
)

data class FilteringData(
    val totalCount: Int,
    val yesPercentage: Double,
    val noPercentage: Double
)

data class QualityDashboard(
    val questionId: Long,
    val demographics: VoteDemographicsReport,
    val gapAnalysis: VoteBetGapReport,
    val filteringEffect: FilteringEffectReport,
    val overallQualityScore: Double
)
