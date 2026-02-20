package com.predata.backend.dto

import com.predata.backend.domain.Choice

// Weighted vote result
data class WeightedVoteResult(
    val rawYesPercentage: Double,      // Original percentage
    val weightedYesPercentage: Double, // Weighted percentage
    val totalVotes: Int,
    val effectiveVotes: Double          // Effective vote count with weights applied
)

// Suspicious group
data class SuspiciousGroup(
    val criteria: String,              // "country=KR"
    val voteYesPercentage: Double,
    val betYesPercentage: Double,
    val gapIndex: Double,              // Discrepancy rate
    val memberCount: Int,
    val riskLevel: RiskLevel
)

enum class RiskLevel { 
    LOW,      // < 15%
    MEDIUM,   // 15-30%
    HIGH,     // 30-50%
    CRITICAL  // > 50%
}

// Abusing report
data class AbusingReport(
    val questionId: Long,
    val suspiciousGroups: List<SuspiciousGroup>,
    val overallGap: Double,
    val totalMembers: Int,
    val suspiciousMembers: Int,
    val recommendation: String
)

// Premium data
data class PremiumDataResponse(
    val questionId: Long,
    val rawVoteCount: Int,
    val cleanedVoteCount: Int,
    val weightedResult: WeightedVoteResult,
    val qualityScore: Double,          // 0-100 score
    val gapReduction: Double           // Discrepancy rate difference before/after filtering
)

// Filtering options
data class FilterOptions(
    val minLatencyMs: Int = 2000,      // Minimum response time
    val onlyBettors: Boolean = false,  // Only bettors
    val minTierWeight: Double = 1.0,   // Minimum tier weight
    val excludeCountries: List<String> = emptyList()
)

data class QualityScoreResponse(
    val questionId: Long,
    val qualityScore: Double,
    val grade: String
)

data class FastClickersResponse(
    val questionId: Long,
    val thresholdMs: Int,
    val suspiciousCount: Int,
    val percentage: Double
)

data class FilterSimulationResponse(
    val questionId: Long,
    val filterOptions: FilterOptions,
    val originalVoteCount: Int,
    val filteredVoteCount: Int,
    val removedCount: Int,
    val originalYesPercentage: Double,
    val filteredYesPercentage: Double,
    val percentageChange: Double
)
