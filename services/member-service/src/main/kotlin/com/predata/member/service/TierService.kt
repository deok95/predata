package com.predata.member.service

import com.predata.common.domain.Tier
import com.predata.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TierService(
    private val memberRepository: MemberRepository
) {

    companion object {
        // 티어별 임계치
        const val BRONZE_TO_SILVER = 100
        const val SILVER_TO_GOLD = 300
        const val GOLD_TO_PLATINUM = 600

        // 티어별 가중치
        val TIER_WEIGHTS = mapOf(
            Tier.BRONZE to BigDecimal("1.00"),
            Tier.SILVER to BigDecimal("1.25"),
            Tier.GOLD to BigDecimal("1.50"),
            Tier.PLATINUM to BigDecimal("2.00")
        )

        // 점수 계산
        const val CORRECT_PREDICTION_POINTS = 10
        const val WRONG_PREDICTION_PENALTY = -3
    }

    /**
     * 개별 회원의 티어 업데이트 (이벤트 기반)
     */
    @Transactional
    fun updateMemberTier(memberId: Long, isCorrect: Boolean) {
        val member = memberRepository.findById(memberId).orElse(null) ?: return

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

        memberRepository.save(member)
    }

    /**
     * 점수에 따른 티어 계산
     */
    fun calculateTier(score: Int): Tier {
        return when {
            score >= GOLD_TO_PLATINUM -> Tier.PLATINUM
            score >= SILVER_TO_GOLD -> Tier.GOLD
            score >= BRONZE_TO_SILVER -> Tier.SILVER
            else -> Tier.BRONZE
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
            Tier.BRONZE -> Tier.SILVER to BRONZE_TO_SILVER
            Tier.SILVER -> Tier.GOLD to SILVER_TO_GOLD
            Tier.GOLD -> Tier.PLATINUM to GOLD_TO_PLATINUM
            Tier.PLATINUM -> Tier.PLATINUM to GOLD_TO_PLATINUM // 최고 티어
        }

        val currentTierThreshold = when (currentTier) {
            Tier.BRONZE -> 0
            Tier.SILVER -> BRONZE_TO_SILVER
            Tier.GOLD -> SILVER_TO_GOLD
            Tier.PLATINUM -> GOLD_TO_PLATINUM
        }

        val progressPercentage = if (currentTier == Tier.PLATINUM) {
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
            currentTier = currentTier.name,
            currentScore = currentScore,
            nextTier = nextTier.name,
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
            bronzeCount = tierCounts[Tier.BRONZE] ?: 0,
            silverCount = tierCounts[Tier.SILVER] ?: 0,
            goldCount = tierCounts[Tier.GOLD] ?: 0,
            platinumCount = tierCounts[Tier.PLATINUM] ?: 0,
            averageAccuracyScore = if (allMembers.isNotEmpty()) allMembers.map { it.accuracyScore }.average().toInt() else 0
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
