package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.dto.WeightedVoteResult
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class PersonaWeightService(
    private val activityRepository: ActivityRepository,
    private val memberRepository: MemberRepository
) {

    /**
     * 페르소나별 가중치를 적용한 투표 결과 계산
     * 최적화: 회원 정보를 일괄 조회하여 N+1 문제 해결
     */
    @Transactional(readOnly = true)
    @Cacheable(value = ["weightedVotes"], key = "#questionId + '_' + #category", unless = "#result.totalVotes == 0")
    fun calculateWeightedVotes(
        questionId: Long,
        category: String? = null
    ): WeightedVoteResult {
        val votes = activityRepository.findByQuestionIdAndActivityType(
            questionId,
            ActivityType.VOTE
        )

        if (votes.isEmpty()) {
            return WeightedVoteResult(
                rawYesPercentage = 0.0,
                weightedYesPercentage = 0.0,
                totalVotes = 0,
                effectiveVotes = 0.0
            )
        }

        // 회원 정보 일괄 조회 (N+1 해결)
        val memberIds = votes.map { it.memberId }.distinct()
        val membersMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }

        var yesWeightSum = 0.0
        var noWeightSum = 0.0

        votes.forEach { vote ->
            val member = membersMap[vote.memberId] ?: return@forEach

            // 기본 티어 가중치
            val tierWeight = member.tierWeight.toDouble()

            // 직업별 보너스 (질문 카테고리에 따라)
            val jobBonus = calculateJobBonus(member.jobCategory, category)

            // 연령대별 가중치
            val ageBonus = calculateAgeBonus(member.ageGroup)

            val finalWeight = tierWeight * jobBonus * ageBonus

            if (vote.choice == Choice.YES) {
                yesWeightSum += finalWeight
            } else {
                noWeightSum += finalWeight
            }
        }

        val totalWeight = yesWeightSum + noWeightSum

        // 원본 비율
        val rawYesCount = votes.count { it.choice == Choice.YES }
        val rawYesPct = rawYesCount * 100.0 / votes.size

        // 가중치 적용 비율
        val weightedYesPct = if (totalWeight > 0) {
            (yesWeightSum / totalWeight) * 100
        } else {
            rawYesPct
        }

        return WeightedVoteResult(
            rawYesPercentage = BigDecimal(rawYesPct).setScale(2, RoundingMode.HALF_UP).toDouble(),
            weightedYesPercentage = BigDecimal(weightedYesPct).setScale(2, RoundingMode.HALF_UP).toDouble(),
            totalVotes = votes.size,
            effectiveVotes = BigDecimal(totalWeight).setScale(2, RoundingMode.HALF_UP).toDouble()
        )
    }

    /**
     * 직업별 보너스 계산
     */
    private fun calculateJobBonus(jobCategory: String?, questionCategory: String?): Double {
        if (jobCategory == null) return 1.0

        return when (questionCategory?.uppercase()) {
            "ECONOMY", "FINANCE" -> when (jobCategory) {
                "Finance" -> 1.5   // 금융인은 경제 질문에 +50%
                "IT" -> 1.0
                "Student" -> 0.8
                else -> 0.9
            }
            "TECHNOLOGY", "IT" -> when (jobCategory) {
                "IT" -> 1.5        // IT인은 기술 질문에 +50%
                "Finance" -> 1.0
                "Student" -> 1.2   // 학생들도 기술에 관심
                else -> 0.9
            }
            "MEDICAL", "HEALTH" -> when (jobCategory) {
                "Medical" -> 1.5   // 의료인은 건강 질문에 +50%
                else -> 1.0
            }
            else -> 1.0           // 기타 카테고리는 동일
        }
    }

    /**
     * 연령대별 가중치
     */
    private fun calculateAgeBonus(ageGroup: Int?): Double {
        return when (ageGroup) {
            null -> 0.8         // 연령 미공개는 신뢰도 낮음
            in 20..29 -> 0.9    // 20대
            in 30..39 -> 1.2    // 30대 (경제활동 활발, 높은 신뢰도)
            in 40..49 -> 1.1    // 40대
            in 50..59 -> 1.0    // 50대
            else -> 0.8         // 기타
        }
    }

    /**
     * 국가별 가중치 적용 투표
     * 최적화: 회원 정보를 한 번만 조회하여 국가 추출
     */
    @Transactional(readOnly = true)
    @Cacheable(value = ["votesByCountry"], key = "#questionId")
    fun calculateVotesByCountry(questionId: Long): Map<String, WeightedVoteResult> {
        val votes = activityRepository.findByQuestionIdAndActivityType(
            questionId,
            ActivityType.VOTE
        )

        if (votes.isEmpty()) {
            return emptyMap()
        }

        // 회원 정보 일괄 조회 (N+1 해결)
        val memberIds = votes.map { it.memberId }.distinct()
        val membersMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }

        // 국가별로 투표 그룹화
        val votesByCountry = votes.groupBy { vote ->
            membersMap[vote.memberId]?.countryCode ?: "UNKNOWN"
        }.filterKeys { it != "UNKNOWN" }

        // 각 국가별 결과 계산
        return votesByCountry.mapValues { (_, countryVotes) ->
            val rawYesCount = countryVotes.count { it.choice == Choice.YES }
            val rawYesPct = if (countryVotes.isNotEmpty()) {
                rawYesCount * 100.0 / countryVotes.size
            } else 0.0

            WeightedVoteResult(
                rawYesPercentage = BigDecimal(rawYesPct).setScale(2, RoundingMode.HALF_UP).toDouble(),
                weightedYesPercentage = rawYesPct, // 국가별은 단순 집계
                totalVotes = countryVotes.size,
                effectiveVotes = countryVotes.size.toDouble()
            )
        }
    }
}
