package com.predata.backend.domain.policy

object SybilDetectionPolicy {
    const val suspiciousIpThreshold: Int = 5
    const val minVotesForPatternCheck: Int = 5

    fun shouldFlagSuspiciousIp(accountCount: Int, threshold: Int = suspiciousIpThreshold): Boolean =
        accountCount >= threshold

    fun isUniformChoicePattern(totalVotes: Int, uniqueChoiceCount: Int, minVotes: Int = minVotesForPatternCheck): Boolean =
        totalVotes >= minVotes && uniqueChoiceCount == 1

    fun ipReason(): String = "동일 IP에서 다수 계정 투표"

    fun ipDetail(ip: String, accountCount: Int): String = "IP $ip 에서 ${accountCount}개 계정이 투표"

    fun uniformChoiceReason(choiceName: String): String = "동일 선택 패턴 (${choiceName}만 선택)"

    fun uniformChoiceDetail(totalVotes: Int, choiceName: String): String =
        "총 ${totalVotes}개 투표가 모두 ${choiceName}"
}
