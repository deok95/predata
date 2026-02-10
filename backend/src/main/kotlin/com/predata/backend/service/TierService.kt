package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TierService(
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository
) {

    companion object {
        // 티어별 임계치
        const val BRONZE_TO_SILVER = 100
        const val SILVER_TO_GOLD = 300
        const val GOLD_TO_PLATINUM = 600

        // 티어별 가중치
        val TIER_WEIGHTS = mapOf(
            "BRONZE" to BigDecimal("1.00"),
            "SILVER" to BigDecimal("1.25"),
            "GOLD" to BigDecimal("1.50"),
            "PLATINUM" to BigDecimal("2.00")
        )

        // 점수 계산
        const val CORRECT_PREDICTION_POINTS = 10
        const val WRONG_PREDICTION_PENALTY = -3
    }

    /**
     * 정산 시 모든 참여자의 티어 업데이트
     * 최적화: N+1 → 배치 조회, 개별 save → saveAll
     */
    @Transactional
    fun updateTiersAfterSettlement(questionId: Long, finalResult: FinalResult) {
        // 1. 질문에 투표한 모든 사용자 조회
        val votes = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
        if (votes.isEmpty()) return

        val winningChoice = if (finalResult == FinalResult.YES) Choice.YES else Choice.NO

        // 2. 배치 조회: N+1 → 1회 조회
        val memberIds = votes.map { it.memberId }.distinct()
        val membersMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }
        val updatedMembers = mutableListOf<com.predata.backend.domain.Member>()

        // 3. 각 사용자의 정확도 업데이트
        votes.forEach { vote ->
            val member = membersMap[vote.memberId] ?: return@forEach

            val isCorrect = vote.choice == winningChoice

            // 점수 업데이트
            if (isCorrect) {
                member.accuracyScore += CORRECT_PREDICTION_POINTS
                member.correctPredictions++
            } else {
                member.accuracyScore += WRONG_PREDICTION_PENALTY
            }
            member.totalPredictions++

            // 점수가 음수가 되지 않도록
            if (member.accuracyScore < 0) {
                member.accuracyScore = 0
            }

            // 티어 업그레이드 확인
            val newTier = calculateTier(member.accuracyScore)
            if (newTier != member.tier) {
                member.tier = newTier
                member.tierWeight = TIER_WEIGHTS[newTier] ?: BigDecimal("1.00")
            }

            updatedMembers.add(member)
        }

        // 4. 일괄 저장
        memberRepository.saveAll(updatedMembers)
    }

    /**
     * 점수에 따른 티어 계산
     */
    fun calculateTier(score: Int): String {
        return when {
            score >= GOLD_TO_PLATINUM -> "PLATINUM"
            score >= SILVER_TO_GOLD -> "GOLD"
            score >= BRONZE_TO_SILVER -> "SILVER"
            else -> "BRONZE"
        }
    }

    /**
     * 사용자의 티어 진행도 조회
     */
    @Transactional(readOnly = true)
    fun getTierProgress(memberId: Long): TierProgressResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        val currentScore = member.accuracyScore
        val currentTier = member.tier

        // 다음 티어까지 필요한 점수
        val (nextTier, nextTierThreshold) = when (currentTier) {
            "BRONZE" -> "SILVER" to BRONZE_TO_SILVER
            "SILVER" -> "GOLD" to SILVER_TO_GOLD
            "GOLD" -> "PLATINUM" to GOLD_TO_PLATINUM
            "PLATINUM" -> "PLATINUM" to GOLD_TO_PLATINUM // 최고 티어
            else -> "SILVER" to BRONZE_TO_SILVER
        }

        val currentTierThreshold = when (currentTier) {
            "BRONZE" -> 0
            "SILVER" -> BRONZE_TO_SILVER
            "GOLD" -> SILVER_TO_GOLD
            "PLATINUM" -> GOLD_TO_PLATINUM
            else -> 0
        }

        val progressPercentage = if (currentTier == "PLATINUM") {
            100.0
        } else {
            val rangeSize = nextTierThreshold - currentTierThreshold
            val progress = currentScore - currentTierThreshold
            (progress.toDouble() / rangeSize * 100).coerceIn(0.0, 100.0)
        }

        val accuracyPercentage = if (member.totalPredictions > 0) {
            (member.correctPredictions.toDouble() / member.totalPredictions * 100)
        } else 0.0

        return TierProgressResponse(
            memberId = memberId,
            currentTier = currentTier,
            currentScore = currentScore,
            nextTier = nextTier,
            nextTierThreshold = nextTierThreshold,
            progressPercentage = progressPercentage,
            tierWeight = member.tierWeight.toDouble(),
            totalPredictions = member.totalPredictions,
            correctPredictions = member.correctPredictions,
            accuracyPercentage = accuracyPercentage
        )
    }

    /**
     * 전체 티어 통계
     */
    @Transactional(readOnly = true)
    fun getTierStatistics(): TierStatistics {
        val allMembers = memberRepository.findAll()

        val tierCounts = allMembers.groupingBy { it.tier }.eachCount()
        val totalMembers = allMembers.size

        return TierStatistics(
            totalMembers = totalMembers,
            bronzeCount = tierCounts["BRONZE"] ?: 0,
            silverCount = tierCounts["SILVER"] ?: 0,
            goldCount = tierCounts["GOLD"] ?: 0,
            platinumCount = tierCounts["PLATINUM"] ?: 0,
            averageAccuracyScore = allMembers.map { it.accuracyScore }.average().toInt()
        )
    }
}

// ===== DTOs =====

data class TierProgressResponse(
    val memberId: Long,
    val currentTier: String,
    val currentScore: Int,
    val nextTier: String,
    val nextTierThreshold: Int,
    val progressPercentage: Double,
    val tierWeight: Double,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val accuracyPercentage: Double
)

data class TierStatistics(
    val totalMembers: Int,
    val bronzeCount: Int,
    val silverCount: Int,
    val goldCount: Int,
    val platinumCount: Int,
    val averageAccuracyScore: Int
)
