package com.predata.backend.service

import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeaderboardService(
    private val memberRepository: MemberRepository
) {

    @Transactional(readOnly = true)
    fun getTopPredictors(limit: Int = 20): List<LeaderboardEntry> {
        return memberRepository.findAll()
            .filter { it.totalPredictions > 0 }
            .sortedByDescending { it.accuracyScore }
            .take(limit)
            .mapIndexed { index, member ->
                val accuracy = if (member.totalPredictions > 0)
                    (member.correctPredictions.toDouble() / member.totalPredictions * 100)
                else 0.0

                LeaderboardEntry(
                    rank = index + 1,
                    memberId = member.id!!,
                    email = maskEmail(member.email),
                    tier = member.tier,
                    accuracyScore = member.accuracyScore,
                    accuracyPercentage = accuracy,
                    totalPredictions = member.totalPredictions,
                    correctPredictions = member.correctPredictions,
                    pointBalance = member.pointBalance
                )
            }
    }

    @Transactional(readOnly = true)
    fun getMemberRank(memberId: Long): LeaderboardEntry? {
        val sorted = memberRepository.findAll()
            .filter { it.totalPredictions > 0 }
            .sortedByDescending { it.accuracyScore }

        val index = sorted.indexOfFirst { it.id == memberId }
        if (index < 0) return null

        val member = sorted[index]
        val accuracy = if (member.totalPredictions > 0)
            (member.correctPredictions.toDouble() / member.totalPredictions * 100)
        else 0.0

        return LeaderboardEntry(
            rank = index + 1,
            memberId = member.id!!,
            email = maskEmail(member.email),
            tier = member.tier,
            accuracyScore = member.accuracyScore,
            accuracyPercentage = accuracy,
            totalPredictions = member.totalPredictions,
            correctPredictions = member.correctPredictions,
            pointBalance = member.pointBalance
        )
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2 || parts[0].length <= 2) return "***@${parts.getOrElse(1) { "***" }}"
        val name = parts[0]
        return "${name.take(2)}${"*".repeat((name.length - 2).coerceAtMost(5))}@${parts[1]}"
    }
}

data class LeaderboardEntry(
    val rank: Int,
    val memberId: Long,
    val email: String,
    val tier: String,
    val accuracyScore: Int,
    val accuracyPercentage: Double,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val pointBalance: Long
)
