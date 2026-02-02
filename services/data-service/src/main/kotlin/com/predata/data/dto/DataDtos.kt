package com.predata.data.dto

/**
 * 글로벌 통계 응답 DTO
 */
data class GlobalStatsDto(
    val totalPredictions: Int,      // 총 질문 수
    val tvl: Long,                   // Total Value Locked (총 베팅 금액)
    val activeUsers: Int,            // 활성 유저 수
    val cumulativeRewards: Long,     // 누적 보상 금액
    val activeMarkets: Int           // 진행 중인 마켓 수
)

/**
 * 프리미엄 데이터 응답 DTO
 */
data class PremiumDataDto(
    val questionId: Long,
    val personaDistribution: Map<String, Int>,  // 페르소나별 베팅 분포
    val countryDistribution: Map<String, Int>,  // 국가별 베팅 분포
    val tierDistribution: Map<String, Int>      // 티어별 베팅 분포
)

/**
 * 데이터 품질 점수 DTO
 */
data class DataQualityScoreDto(
    val questionId: Long,
    val diversityScore: Double,      // 페르소나 다양성 점수
    val volumeScore: Double,         // 베팅 볼륨 점수
    val speedScore: Double,          // 응답 속도 점수
    val overallScore: Double         // 종합 점수
)
