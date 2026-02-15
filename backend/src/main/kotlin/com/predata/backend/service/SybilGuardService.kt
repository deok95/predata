package com.predata.backend.service

import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.VoteCommitRepository
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 시빌 공격 완화 설정
 */
@Component
@ConfigurationProperties(prefix = "app.sybil")
data class SybilGuardConfig(
    var minAccountAgeDays: Int = 3,
    var minVoteHistory: Int = 3
)

/**
 * 시빌 공격 완화 서비스
 * - 보상 대상 최소 조건 체크
 * - 계정 생성 후 최소 일수 경과
 * - 최소 투표 이력 (다른 질문)
 */
@Service
class SybilGuardService(
    private val memberRepository: MemberRepository,
    private val voteCommitRepository: VoteCommitRepository,
    private val config: SybilGuardConfig
) {
    /**
     * 보상 대상 여부 체크
     * - 계정 생성 후 최소 일수 경과
     * - 최소 투표 이력 (다른 질문에 투표)
     *
     * @param memberId 회원 ID
     * @return 보상 대상 여부 (true: 보상 가능, false: 보상 불가)
     */
    fun isEligibleForReward(memberId: Long): Boolean {
        // 회원 정보 조회
        val member = memberRepository.findById(memberId).orElse(null) ?: return false

        // 1. 계정 생성 후 최소 일수 경과 체크
        val accountAge = java.time.Duration.between(member.createdAt, LocalDateTime.now(ZoneOffset.UTC)).toDays()
        if (accountAge < config.minAccountAgeDays) {
            return false
        }

        // 2. 최소 투표 이력 체크 (다른 질문에 투표한 수)
        val voteCount = voteCommitRepository.countDistinctQuestionsByMemberId(memberId)
        if (voteCount < config.minVoteHistory) {
            return false
        }

        return true
    }

    /**
     * 특정 질문에 대한 보상 대상 회원 필터링
     * @param memberIds 회원 ID 목록
     * @return 보상 대상 회원 ID 목록
     */
    fun filterEligibleMembers(memberIds: List<Long>): List<Long> {
        return memberIds.filter { isEligibleForReward(it) }
    }
}
