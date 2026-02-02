package com.predata.member.dto

import com.predata.common.domain.Tier
import com.predata.member.domain.Member
import java.math.BigDecimal
import java.time.LocalDateTime

data class MemberResponse(
    val id: Long,
    val email: String,
    val walletAddress: String?,
    val countryCode: String,
    val jobCategory: String?,
    val ageGroup: Int?,
    val tier: String,
    val tierWeight: BigDecimal,
    val accuracyScore: Int,
    val pointBalance: Long,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: Member): MemberResponse {
            return MemberResponse(
                id = member.id ?: 0,
                email = member.email,
                walletAddress = member.walletAddress,
                countryCode = member.countryCode,
                jobCategory = member.jobCategory,
                ageGroup = member.ageGroup,
                tier = member.tier.name,
                tierWeight = member.tierWeight,
                accuracyScore = member.accuracyScore,
                pointBalance = member.pointBalance,
                totalPredictions = member.totalPredictions,
                correctPredictions = member.correctPredictions,
                createdAt = member.createdAt
            )
        }
    }
}

/**
 * 티켓 상태 응답 DTO
 */
data class TicketStatusDto(
    val remainingCount: Int,
    val resetDate: String
)
