package com.predata.backend.domain.policy

import com.predata.backend.dto.RiskLevel

object AbusingDetectionPolicy {
    val defaultCountries: List<String> = listOf("KR", "US", "JP", "SG", "VN")
    val defaultJobs: List<String> = listOf("IT", "Finance", "Student", "Medical", "Service")
    val defaultAgeGroups: List<Int> = listOf(20, 30, 40, 50)

    const val gapSuspiciousThreshold: Double = 15.0
    const val fastClickLatencyMs: Int = 1000
    const val fastClickSuspiciousCountThreshold: Int = 500

    fun shouldIncludeGap(gapIndex: Double): Boolean = gapIndex > gapSuspiciousThreshold

    fun isFastClicker(latencyMs: Int?): Boolean = latencyMs != null && latencyMs < fastClickLatencyMs

    fun shouldIncludeFastClickers(memberCount: Int): Boolean = memberCount > fastClickSuspiciousCountThreshold

    fun classifyGapRisk(gap: Double): RiskLevel {
        return when {
            gap > 50 -> RiskLevel.CRITICAL
            gap > 30 -> RiskLevel.HIGH
            gap > 15 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    fun classifyFastClickerRisk(memberCount: Int): RiskLevel {
        return when {
            memberCount > 2000 -> RiskLevel.CRITICAL
            memberCount > 1000 -> RiskLevel.HIGH
            else -> RiskLevel.MEDIUM
        }
    }

    fun format2(value: Double): Double = "%.2f".format(value).toDouble()

    fun recommendation(criticalCount: Int, highCount: Int, overallGap: Double): String {
        return when {
            criticalCount > 0 ->
                "⚠️ CRITICAL: ${criticalCount}개 그룹에서 극단적 괴리 발견. 해당 그룹 데이터 제외 권장."
            highCount > 2 ->
                "⚠️ HIGH: ${highCount}개 그룹에서 높은 괴리 발견. 필터링 후 사용 권장."
            overallGap > 20 ->
                "⚠️ 전체 괴리율 ${"%.1f".format(overallGap)}%. Latency 필터링 권장."
            overallGap < 10 ->
                "✅ 전체 괴리율 ${"%.1f".format(overallGap)}%. 데이터 품질 양호."
            else ->
                "✅ 전체 괴리율 ${"%.1f".format(overallGap)}%. 사용 가능한 수준."
        }
    }
}
