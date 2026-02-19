package com.predata.backend.service

import com.predata.backend.config.RewardConfig
import com.predata.backend.domain.VoteCommitStatus
import com.predata.backend.dto.RewardCalculation
import com.predata.backend.dto.RewardCalculationSummary
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.VoteCommitRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 투표 리워드 계산 서비스
 * - 레벨 기반 가중치 적용
 * - 결정론적 계산 (동일 입력 → 동일 결과)
 * - 0.01 미만 절사, 잔여분 리저브로
 */
@Service
class VoteRewardService(
    private val voteCommitRepository: VoteCommitRepository,
    private val memberRepository: MemberRepository,
    private val feePoolService: FeePoolService,
    private val rewardConfig: RewardConfig,
    private val sybilGuardService: SybilGuardService
) {
    private val logger = LoggerFactory.getLogger(VoteRewardService::class.java)

    /**
     * 보상 계산
     * - 1. 리워드 풀 잔액 조회
     * - 2. 투표자별 레벨 가중치 합산
     * - 3. 1인당 보상 = (리워드풀 × 가중치) / 가중치합계
     * - 4. 0.01 미만 절사, 잔여분 리저브로
     * - 5. 캡: 총 분배액 <= 리워드 풀 잔액
     */
    @Transactional(readOnly = true)
    fun calculateRewards(questionId: Long): RewardCalculationSummary {
        // 1. 리워드 풀 잔액 조회
        val rewardPool = feePoolService.getAvailableRewardPool(questionId)

        if (rewardPool <= BigDecimal.ZERO) {
            logger.warn("No reward pool available for questionId=$questionId")
            return RewardCalculationSummary(
                questionId = questionId,
                totalRewardPool = BigDecimal.ZERO,
                totalWeightSum = BigDecimal.ZERO,
                individualRewards = emptyList(),
                totalDistributed = BigDecimal.ZERO,
                truncatedAmount = BigDecimal.ZERO,
                participantCount = 0
            )
        }

        // 2. REVEALED 상태인 투표자 조회 (memberId 기준 정렬로 결정론적 순서 보장)
        val revealedVotes = voteCommitRepository.findByQuestionIdAndStatusOrderByMemberIdAsc(
            questionId, VoteCommitStatus.REVEALED
        )

        if (revealedVotes.isEmpty()) {
            logger.warn("No revealed votes for questionId=$questionId")
            return RewardCalculationSummary(
                questionId = questionId,
                totalRewardPool = rewardPool,
                totalWeightSum = BigDecimal.ZERO,
                individualRewards = emptyList(),
                totalDistributed = BigDecimal.ZERO,
                truncatedAmount = BigDecimal.ZERO,
                participantCount = 0
            )
        }

        // 3. 시빌 가드: 보상 대상 필터링
        val allMemberIds = revealedVotes.map { it.memberId }
        val eligibleMemberIds = sybilGuardService.filterEligibleMembers(allMemberIds)
        val eligibleMemberIdSet = eligibleMemberIds.toSet()

        logger.info("Sybil guard applied: questionId=$questionId, total=${allMemberIds.size}, eligible=${eligibleMemberIds.size}")

        // 4. 투표자별 레벨 조회 및 가중치 합산 (보상 대상만)
        val members = memberRepository.findAllById(eligibleMemberIds).associateBy { it.id }

        var totalWeightSum = BigDecimal.ZERO
        val weightMap = mutableMapOf<Long, BigDecimal>()

        for (vote in revealedVotes) {
            // 보상 대상이 아니면 스킵
            if (vote.memberId !in eligibleMemberIdSet) continue

            val member = members[vote.memberId] ?: continue
            val weight = rewardConfig.getWeight(member.level)
            weightMap[vote.memberId] = weight
            totalWeightSum = totalWeightSum.add(weight)
        }

        if (totalWeightSum <= BigDecimal.ZERO) {
            logger.warn("Total weight sum is zero for questionId=$questionId")
            return RewardCalculationSummary(
                questionId = questionId,
                totalRewardPool = rewardPool,
                totalWeightSum = BigDecimal.ZERO,
                individualRewards = emptyList(),
                totalDistributed = BigDecimal.ZERO,
                truncatedAmount = BigDecimal.ZERO,
                participantCount = revealedVotes.size
            )
        }

        // 5. 개인별 보상 계산: (리워드풀 × 가중치) / 가중치합계
        val individualRewards = mutableListOf<RewardCalculation>()
        var totalDistributed = BigDecimal.ZERO
        var totalTruncated = BigDecimal.ZERO

        for (vote in revealedVotes) {
            // 보상 대상이 아니면 스킵
            if (vote.memberId !in eligibleMemberIdSet) continue

            val member = members[vote.memberId] ?: continue
            val weight = weightMap[vote.memberId] ?: BigDecimal.ZERO

            // 보상 = (리워드풀 × 가중치) / 가중치합계, 6자리 소수점 반올림
            val rawReward = rewardPool
                .multiply(weight)
                .divide(totalWeightSum, 6, RoundingMode.HALF_UP)

            // 0.01 미만 절사
            val truncatedReward = if (rawReward >= rewardConfig.minPayout) {
                rawReward.setScale(2, RoundingMode.DOWN)
            } else {
                BigDecimal.ZERO
            }

            val truncated = rawReward.subtract(truncatedReward)
            totalTruncated = totalTruncated.add(truncated)

            if (truncatedReward > BigDecimal.ZERO) {
                individualRewards.add(
                    RewardCalculation(
                        memberId = vote.memberId,
                        level = member.level,
                        weight = weight,
                        rewardAmount = truncatedReward,
                        questionId = questionId
                    )
                )
                totalDistributed = totalDistributed.add(truncatedReward)
            }
        }

        // 6. 캡: 총 분배액 <= 리워드 풀 잔액 검증
        if (totalDistributed > rewardPool) {
            logger.error("Total distributed ($totalDistributed) exceeds reward pool ($rewardPool) for questionId=$questionId")
            throw IllegalStateException("Total rewards exceed reward pool balance.")
        }

        logger.info("Reward calculation complete: questionId=$questionId, pool=$rewardPool, distributed=$totalDistributed, truncated=$totalTruncated, participants=${individualRewards.size}")

        return RewardCalculationSummary(
            questionId = questionId,
            totalRewardPool = rewardPool,
            totalWeightSum = totalWeightSum,
            individualRewards = individualRewards,
            totalDistributed = totalDistributed,
            truncatedAmount = totalTruncated,
            participantCount = revealedVotes.size
        )
    }
}
