package com.predata.backend.dto

import com.predata.backend.domain.Choice

// 가중치 적용 투표 결과
data class WeightedVoteResult(
    val rawYesPercentage: Double,      // 원본 비율
    val weightedYesPercentage: Double, // 가중치 적용 비율
    val totalVotes: Int,
    val effectiveVotes: Double          // 가중치 적용된 유효 투표수
)

// 의심 그룹
data class SuspiciousGroup(
    val criteria: String,              // "country=KR"
    val voteYesPercentage: Double,     
    val betYesPercentage: Double,      
    val gapIndex: Double,              // 괴리율
    val memberCount: Int,
    val riskLevel: RiskLevel
)

enum class RiskLevel { 
    LOW,      // < 15%
    MEDIUM,   // 15-30%
    HIGH,     // 30-50%
    CRITICAL  // > 50%
}

// 어뷰징 리포트
data class AbusingReport(
    val questionId: Long,
    val suspiciousGroups: List<SuspiciousGroup>,
    val overallGap: Double,
    val totalMembers: Int,
    val suspiciousMembers: Int,
    val recommendation: String
)

// 프리미엄 데이터
data class PremiumDataResponse(
    val questionId: Long,
    val rawVoteCount: Int,
    val cleanedVoteCount: Int,
    val weightedResult: WeightedVoteResult,
    val qualityScore: Double,          // 0-100점
    val gapReduction: Double           // 필터링 전후 괴리율 차이
)

// 필터링 옵션
data class FilterOptions(
    val minLatencyMs: Int = 2000,      // 최소 응답 시간
    val onlyBettors: Boolean = false,  // 베팅 경험자만
    val minTierWeight: Double = 1.0,   // 최소 티어 가중치
    val excludeCountries: List<String> = emptyList()
)
