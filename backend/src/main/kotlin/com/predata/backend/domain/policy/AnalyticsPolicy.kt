package com.predata.backend.domain.policy

import kotlin.math.abs
import kotlin.math.max

object AnalyticsPolicy {
    const val DEFAULT_SUSPICIOUS_LATENCY_MS = 2000

    fun percentage(part: Int, total: Int): Double =
        if (total > 0) (part.toDouble() / total.toDouble() * 100.0) else 0.0

    fun percentage(part: Long, total: Long): Double =
        if (total > 0L) (part.toDouble() / total.toDouble() * 100.0) else 0.0

    fun gapPercentage(voteYesPercentage: Double, betYesPercentage: Double): Double =
        abs(voteYesPercentage - betYesPercentage)

    fun qualityScore(gapPercentage: Double): Double {
        return when {
            gapPercentage < 5.0 -> 95.0 + (5.0 - gapPercentage)
            gapPercentage < 10.0 -> 85.0 + (10.0 - gapPercentage)
            gapPercentage < 20.0 -> 70.0 + (20.0 - gapPercentage) * 0.75
            else -> max(0.0, 70.0 - (gapPercentage - 20.0))
        }
    }

    fun isSuspiciousLatency(latencyMs: Int?, thresholdMs: Int = DEFAULT_SUSPICIOUS_LATENCY_MS): Boolean =
        (latencyMs ?: Int.MAX_VALUE) < thresholdMs
}
