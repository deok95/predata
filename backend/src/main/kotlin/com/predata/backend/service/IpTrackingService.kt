package com.predata.backend.service

import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * IP 기반 다중계정 탐지 서비스
 * - 동일 IP에서 가입된 계정 감지
 * - 동일 IP에서 활동하는 복수 계정 감지
 * - IP 이력 추적
 */
@Service
class IpTrackingService(
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository
) {
    private val logger = LoggerFactory.getLogger(IpTrackingService::class.java)

    companion object {
        const val MULTI_ACCOUNT_THRESHOLD = 2 // 같은 IP에서 2개 이상 계정이면 의심
    }

    /**
     * 다중계정 의심 리포트 생성
     * - 동일 signupIp로 가입된 계정 그룹
     * - 동일 lastIp를 사용하는 계정 그룹
     */
    fun detectMultiAccounts(): MultiAccountReport {
        val allMembers = memberRepository.findAll()

        // 가입 IP 기준 그룹핑
        val signupIpGroups = allMembers
            .filter { !it.signupIp.isNullOrBlank() }
            .groupBy { it.signupIp!! }
            .filter { it.value.size >= MULTI_ACCOUNT_THRESHOLD }
            .map { (ip, members) ->
                SuspiciousIpGroup(
                    ip = ip,
                    type = "SIGNUP_IP",
                    accountCount = members.size,
                    accounts = members.map { m ->
                        SuspiciousAccount(
                            memberId = m.id ?: 0,
                            email = m.email,
                            createdAt = m.createdAt.toString(),
                            isBanned = m.isBanned,
                            usdcBalance = m.usdcBalance.toLong()
                        )
                    },
                    riskLevel = when {
                        members.size >= 5 -> "CRITICAL"
                        members.size >= 3 -> "HIGH"
                        else -> "MEDIUM"
                    }
                )
            }

        // 최근 활동 IP 기준 그룹핑
        val lastIpGroups = allMembers
            .filter { !it.lastIp.isNullOrBlank() }
            .groupBy { it.lastIp!! }
            .filter { it.value.size >= MULTI_ACCOUNT_THRESHOLD }
            .map { (ip, members) ->
                SuspiciousIpGroup(
                    ip = ip,
                    type = "LAST_ACTIVE_IP",
                    accountCount = members.size,
                    accounts = members.map { m ->
                        SuspiciousAccount(
                            memberId = m.id ?: 0,
                            email = m.email,
                            createdAt = m.createdAt.toString(),
                            isBanned = m.isBanned,
                            usdcBalance = m.usdcBalance.toLong()
                        )
                    },
                    riskLevel = when {
                        members.size >= 5 -> "CRITICAL"
                        members.size >= 3 -> "HIGH"
                        else -> "MEDIUM"
                    }
                )
            }

        // Activity IP 기반 크로스 계정 탐지
        val activityIpGroups = activityRepository.findAll()
            .filter { !it.ipAddress.isNullOrBlank() }
            .groupBy { it.ipAddress!! }
            .mapValues { (_, activities) -> activities.map { it.memberId }.distinct() }
            .filter { it.value.size >= MULTI_ACCOUNT_THRESHOLD }
            .map { (ip, memberIds) ->
                val members = memberIds.mapNotNull { id -> memberRepository.findById(id).orElse(null) }
                SuspiciousIpGroup(
                    ip = ip,
                    type = "ACTIVITY_IP",
                    accountCount = members.size,
                    accounts = members.map { m ->
                        SuspiciousAccount(
                            memberId = m.id ?: 0,
                            email = m.email,
                            createdAt = m.createdAt.toString(),
                            isBanned = m.isBanned,
                            usdcBalance = m.usdcBalance.toLong()
                        )
                    },
                    riskLevel = when {
                        members.size >= 5 -> "CRITICAL"
                        members.size >= 3 -> "HIGH"
                        else -> "MEDIUM"
                    }
                )
            }

        val allGroups = (signupIpGroups + lastIpGroups + activityIpGroups)
            .distinctBy { "${it.ip}:${it.type}" }
            .sortedByDescending { it.accountCount }

        return MultiAccountReport(
            totalSuspiciousGroups = allGroups.size,
            criticalCount = allGroups.count { it.riskLevel == "CRITICAL" },
            highCount = allGroups.count { it.riskLevel == "HIGH" },
            mediumCount = allGroups.count { it.riskLevel == "MEDIUM" },
            groups = allGroups
        )
    }

    /**
     * 특정 IP의 연결 계정 조회
     */
    fun getAccountsByIp(ip: String): IpLookupResult {
        val bySignup = memberRepository.findBySignupIp(ip)
        val byLastIp = memberRepository.findByLastIp(ip)

        // Activity에서도 조회
        val activityMemberIds = activityRepository.findByIpAddress(ip)
            .map { it.memberId }
            .distinct()
        val byActivity = activityMemberIds.mapNotNull { memberRepository.findById(it).orElse(null) }

        val allMembers = (bySignup + byLastIp + byActivity).distinctBy { it.id }

        return IpLookupResult(
            ip = ip,
            totalAccounts = allMembers.size,
            accounts = allMembers.map { m ->
                IpLinkedAccount(
                    memberId = m.id ?: 0,
                    email = m.email,
                    signupIp = m.signupIp,
                    lastIp = m.lastIp,
                    isBanned = m.isBanned,
                    createdAt = m.createdAt.toString(),
                    matchType = buildList {
                        if (m.signupIp == ip) add("SIGNUP")
                        if (m.lastIp == ip) add("LAST_ACTIVE")
                        if (m.id in activityMemberIds) add("ACTIVITY")
                    }
                )
            }
        )
    }

    /**
     * 특정 유저의 IP 이력
     */
    fun getIpHistoryForMember(memberId: Long): MemberIpHistory {
        val member = memberRepository.findById(memberId).orElse(null)
            ?: return MemberIpHistory(memberId = memberId, ips = emptyList())

        // Activity에서 사용된 모든 IP 수집
        val activityIps = activityRepository.findByMemberId(memberId)
            .mapNotNull { it.ipAddress }
            .distinct()

        val allIps = buildList {
            member.signupIp?.let { add(IpRecord(it, "SIGNUP")) }
            member.lastIp?.let { add(IpRecord(it, "LAST_ACTIVE")) }
            activityIps.forEach { add(IpRecord(it, "ACTIVITY")) }
        }.distinctBy { "${it.ip}:${it.type}" }

        return MemberIpHistory(
            memberId = memberId,
            email = member.email,
            ips = allIps,
            totalUniqueIps = allIps.map { it.ip }.distinct().size
        )
    }

    /**
     * 회원가입 시 동일 IP 가입 이력 체크
     * @return 동일 IP로 가입된 기존 계정 수
     */
    fun checkSignupIp(ip: String): Int {
        return memberRepository.findBySignupIp(ip).size
    }
}

// === DTOs ===

data class MultiAccountReport(
    val totalSuspiciousGroups: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val groups: List<SuspiciousIpGroup>
)

data class SuspiciousIpGroup(
    val ip: String,
    val type: String,
    val accountCount: Int,
    val accounts: List<SuspiciousAccount>,
    val riskLevel: String
)

data class SuspiciousAccount(
    val memberId: Long,
    val email: String,
    val createdAt: String,
    val isBanned: Boolean,
    val usdcBalance: Long
)

data class IpLookupResult(
    val ip: String,
    val totalAccounts: Int,
    val accounts: List<IpLinkedAccount>
)

data class IpLinkedAccount(
    val memberId: Long,
    val email: String,
    val signupIp: String?,
    val lastIp: String?,
    val isBanned: Boolean,
    val createdAt: String,
    val matchType: List<String>
)

data class MemberIpHistory(
    val memberId: Long,
    val email: String? = null,
    val ips: List<IpRecord>,
    val totalUniqueIps: Int = 0
)

data class IpRecord(
    val ip: String,
    val type: String
)
