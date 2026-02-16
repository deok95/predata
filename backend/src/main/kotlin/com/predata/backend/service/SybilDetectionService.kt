package com.predata.backend.service

import com.predata.backend.domain.Choice
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.VoteCommitRepository
import org.springframework.stereotype.Service

/**
 * 시빌 공격 의심 계정 정보
 */
data class SybilSuspiciousAccount(
    val memberId: Long,
    val email: String,
    val reason: String,
    val detail: String
)

/**
 * 시빌 공격 탐지 서비스
 * - 동일 IP에서 다수 계정 투표 탐지
 * - 동일 선택만 하는 계정 탐지
 */
@Service
class SybilDetectionService(
    private val voteCommitRepository: VoteCommitRepository,
    private val memberRepository: MemberRepository
) {
    companion object {
        const val SUSPICIOUS_IP_THRESHOLD = 5  // 동일 IP에서 5개 이상 계정
        const val MIN_VOTES_FOR_PATTERN_CHECK = 5  // 패턴 체크 최소 투표 수
    }

    /**
     * 특정 질문에 대한 의심스러운 패턴 탐지
     *
     * @param questionId 질문 ID
     * @return 의심스러운 계정 목록
     */
    fun detectSuspiciousPatterns(questionId: Long): List<SybilSuspiciousAccount> {
        val suspicious = mutableListOf<SybilSuspiciousAccount>()

        // 1. 동일 IP에서 다수 계정 투표 탐지
        suspicious.addAll(detectSuspiciousIpPatterns(questionId))

        // 2. 동일 선택만 하는 계정 탐지
        suspicious.addAll(detectUniformChoicePatterns(questionId))

        return suspicious
    }

    /**
     * 동일 IP에서 다수 계정이 투표한 패턴 탐지
     */
    private fun detectSuspiciousIpPatterns(questionId: Long): List<SybilSuspiciousAccount> {
        val suspicious = mutableListOf<SybilSuspiciousAccount>()

        // 동일 IP에서 5개 이상 계정이 투표한 경우 조회
        val suspiciousIps = voteCommitRepository.findSuspiciousIpsByQuestionId(
            questionId,
            SUSPICIOUS_IP_THRESHOLD
        )

        for (row in suspiciousIps) {
            val ip = row[0] as String
            val count = (row[1] as Number).toInt()

            // 해당 IP를 사용하는 회원들 조회
            val members = memberRepository.findByLastIp(ip)
            for (member in members) {
                // 해당 회원이 이 질문에 투표했는지 확인
                val hasVoted = voteCommitRepository.existsByMemberIdAndQuestionId(member.id!!, questionId)
                if (hasVoted) {
                    suspicious.add(
                        SybilSuspiciousAccount(
                            memberId = member.id!!,
                            email = member.email,
                            reason = "동일 IP에서 다수 계정 투표",
                            detail = "IP $ip 에서 ${count}개 계정이 투표"
                        )
                    )
                }
            }
        }

        return suspicious
    }

    /**
     * 동일 선택만 하는 계정 탐지 (모든 투표가 YES만 또는 NO만)
     */
    private fun detectUniformChoicePatterns(questionId: Long): List<SybilSuspiciousAccount> {
        val suspicious = mutableListOf<SybilSuspiciousAccount>()

        // 이 질문에 투표한 모든 회원 조회
        val votes = voteCommitRepository.findByQuestionIdAndStatusOrderByMemberIdAsc(
            questionId,
            com.predata.backend.domain.VoteCommitStatus.REVEALED
        )

        for (vote in votes) {
            // 회원의 전체 투표 이력 조회
            val allChoices = voteCommitRepository.findRevealedChoicesByMemberId(vote.memberId)

            // 최소 투표 수 이상이고, 모든 선택이 동일한 경우
            if (allChoices.size >= MIN_VOTES_FOR_PATTERN_CHECK) {
                val uniqueChoices = allChoices.toSet()
                if (uniqueChoices.size == 1) {
                    val member = memberRepository.findById(vote.memberId).orElse(null) ?: continue
                    val choice = uniqueChoices.first()

                    suspicious.add(
                        SybilSuspiciousAccount(
                            memberId = member.id!!,
                            email = member.email,
                            reason = "동일 선택 패턴 (${choice.name}만 선택)",
                            detail = "총 ${allChoices.size}개 투표가 모두 ${choice.name}"
                        )
                    )
                }
            }
        }

        return suspicious
    }
}
