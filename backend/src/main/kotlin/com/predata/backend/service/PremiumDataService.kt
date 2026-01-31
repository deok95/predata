package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class PremiumDataService(
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository,
    private val questionRepository: QuestionRepository
) {

    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    /**
     * 프리미엄 데이터 추출 (필터링 + 세분화)
     */
    @Transactional(readOnly = true)
    fun extractPremiumData(request: PremiumDataRequest): PremiumDataResponse {
        // 1. 질문 조회
        val question = questionRepository.findById(request.questionId)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다.") }

        // 2. 투표 데이터 조회
        val allVotes = activityRepository.findByQuestionIdAndActivityType(
            request.questionId,
            ActivityType.VOTE
        )

        // 3. 필터링 적용
        var filteredVotes = allVotes

        // 3-1. 지연시간 필터링 (어뷰징 제거)
        if (request.minLatencyMs != null) {
            filteredVotes = filteredVotes.filter { vote ->
                (vote.latencyMs ?: Int.MAX_VALUE) >= request.minLatencyMs
            }
        }

        // 3-2. 페르소나 필터링
        filteredVotes = filteredVotes.filter { vote ->
            val member = memberRepository.findById(vote.memberId).orElse(null) ?: return@filter false

            val countryMatch = request.countryCode == null || member.countryCode == request.countryCode
            val jobMatch = request.jobCategory == null || member.jobCategory == request.jobCategory
            val ageMatch = request.ageGroup == null || member.ageGroup == request.ageGroup
            val tierMatch = request.minTier == null || isTierHigherOrEqual(member.tier, request.minTier)

            countryMatch && jobMatch && ageMatch && tierMatch
        }

        // 4. 데이터 추출
        val dataPoints = filteredVotes.mapNotNull { vote ->
            val member = memberRepository.findById(vote.memberId).orElse(null) ?: return@mapNotNull null

            PremiumDataPoint(
                voteId = vote.id ?: 0,
                questionId = question.id ?: 0,
                questionTitle = question.title,
                choice = vote.choice.name,
                latencyMs = vote.latencyMs ?: 0,
                countryCode = member.countryCode,
                jobCategory = member.jobCategory,
                ageGroup = member.ageGroup,
                tier = member.tier,
                tierWeight = member.tierWeight.toDouble(),
                timestamp = vote.createdAt.format(formatter)
            )
        }

        // 5. 통계 계산
        val yesCount = dataPoints.count { it.choice == "YES" }
        val noCount = dataPoints.size - yesCount
        val yesPercentage = if (dataPoints.isNotEmpty()) {
            (yesCount.toDouble() / dataPoints.size * 100)
        } else 0.0

        return PremiumDataResponse(
            questionId = request.questionId,
            questionTitle = question.title,
            filters = request,
            totalCount = dataPoints.size,
            yesCount = yesCount,
            noCount = noCount,
            yesPercentage = yesPercentage,
            noPercentage = 100 - yesPercentage,
            data = dataPoints
        )
    }

    /**
     * 프리미엄 데이터 미리보기 (최대 10개)
     */
    @Transactional(readOnly = true)
    fun previewPremiumData(request: PremiumDataRequest): PremiumDataResponse {
        val fullData = extractPremiumData(request)
        return fullData.copy(data = fullData.data.take(10))
    }

    /**
     * 데이터 품질 요약 (필터별)
     */
    @Transactional(readOnly = true)
    fun getDataQualitySummary(questionId: Long): DataQualitySummary {
        val allVotes = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)

        // 지연시간별 분류
        val instant = allVotes.count { (it.latencyMs ?: Int.MAX_VALUE) < 2000 } // 2초 미만
        val fast = allVotes.count { val ms = it.latencyMs ?: Int.MAX_VALUE; ms >= 2000 && ms < 5000 } // 2-5초
        val normal = allVotes.count { val ms = it.latencyMs ?: Int.MAX_VALUE; ms >= 5000 && ms < 10000 } // 5-10초
        val slow = allVotes.count { (it.latencyMs ?: Int.MAX_VALUE) >= 10000 } // 10초 이상

        // 티어별 분류
        val tierCounts = mutableMapOf<String, Int>()
        allVotes.forEach { vote ->
            val member = memberRepository.findById(vote.memberId).orElse(null)
            if (member != null) {
                tierCounts[member.tier] = tierCounts.getOrDefault(member.tier, 0) + 1
            }
        }

        return DataQualitySummary(
            questionId = questionId,
            totalVotes = allVotes.size,
            byLatency = LatencyBreakdown(
                instant = instant,
                fast = fast,
                normal = normal,
                slow = slow
            ),
            byTier = tierCounts
        )
    }

    /**
     * 티어 비교 (높은 티어인지 확인)
     */
    private fun isTierHigherOrEqual(currentTier: String, minTier: String): Boolean {
        val tierOrder = listOf("BRONZE", "SILVER", "GOLD", "PLATINUM")
        val currentIndex = tierOrder.indexOf(currentTier)
        val minIndex = tierOrder.indexOf(minTier)
        return currentIndex >= minIndex
    }
}

// ===== DTOs =====

data class PremiumDataRequest(
    val questionId: Long,
    val countryCode: String? = null,
    val jobCategory: String? = null,
    val ageGroup: Int? = null,
    val minTier: String? = null, // BRONZE, SILVER, GOLD, PLATINUM
    val minLatencyMs: Int? = null // 어뷰징 필터링 (예: 2000ms 이상만)
)

data class PremiumDataResponse(
    val questionId: Long,
    val questionTitle: String,
    val filters: PremiumDataRequest,
    val totalCount: Int,
    val yesCount: Int,
    val noCount: Int,
    val yesPercentage: Double,
    val noPercentage: Double,
    val data: List<PremiumDataPoint>
)

data class PremiumDataPoint(
    val voteId: Long,
    val questionId: Long,
    val questionTitle: String,
    val choice: String,
    val latencyMs: Int,
    val countryCode: String,
    val jobCategory: String?,
    val ageGroup: Int?,
    val tier: String,
    val tierWeight: Double,
    val timestamp: String
)

data class DataQualitySummary(
    val questionId: Long,
    val totalVotes: Int,
    val byLatency: LatencyBreakdown,
    val byTier: Map<String, Int>
)

data class LatencyBreakdown(
    val instant: Int, // < 2초
    val fast: Int,    // 2-5초
    val normal: Int,  // 5-10초
    val slow: Int     // 10초 이상
)
