package com.predata.backend.service

import com.predata.backend.config.RewardConfig
import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.VoteCommitStatus
import com.predata.backend.dto.RewardCalculation
import com.predata.backend.dto.RewardCalculationSummary
import com.predata.backend.repository.ActivityRepository
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
    private val activityRepository: ActivityRepository,
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

        // Commit-Reveal 미사용 환경 대응: /api/vote(ActivityType.VOTE) 기준 fallback
        val participantIds = if (revealedVotes.isNotEmpty()) {
            revealedVotes.map { it.memberId }
        } else {
            val activityVotes = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
            val ids = activityVotes.map { it.memberId }.distinct().sorted()
            if (ids.isNotEmpty()) {
                logger.info("No revealed votes for questionId=$questionId, fallback to activity votes: participants=${ids.size}")
            }
            ids
        }

        if (participantIds.isEmpty()) {
            logger.warn("No vote participants for questionId=$questionId")
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
        val allMemberIds = participantIds
        val eligibleMemberIdsRaw = sybilGuardService.filterEligibleMembers(allMemberIds)
        // /api/vote 기반 운영 흐름에서 기존 vote_commit 기준 시빌 가드가 전원 탈락시키는 경우가 있어
        // 분배가 완전히 막히지 않도록 fallback 적용
        val eligibleMemberIds = if (eligibleMemberIdsRaw.isEmpty() && allMemberIds.isNotEmpty()) {
            logger.warn("Sybil guard filtered all participants for questionId=$questionId. Fallback to all participants.")
            allMemberIds
        } else {
            eligibleMemberIdsRaw
        }
        val eligibleMemberIdSet = eligibleMemberIds.toSet()

        logger.info("Sybil guard applied: questionId=$questionId, total=${allMemberIds.size}, eligible=${eligibleMemberIds.size}")

        // 4. 투표자별 레벨 조회 및 가중치 합산 (보상 대상만)
        val members = memberRepository.findAllById(eligibleMemberIds).associateBy { it.id }

        var totalWeightSum = BigDecimal.ZERO
        val weightMap = mutableMapOf<Long, BigDecimal>()

        for (memberId in allMemberIds) {
            // 보상 대상이 아니면 스킵
            if (memberId !in eligibleMemberIdSet) continue

            val member = members[memberId] ?: continue
            val weight = rewardConfig.getWeight(member.level)
            weightMap[memberId] = weight
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
                participantCount = allMemberIds.size
            )
        }

        // 5. 개인별 보상 계산: (리워드풀 × 가중치) / 가중치합계
        val individualRewards = mutableListOf<RewardCalculation>()
        var totalDistributed = BigDecimal.ZERO
        var totalTruncated = BigDecimal.ZERO

        for (memberId in allMemberIds) {
            // 보상 대상이 아니면 스킵
            if (memberId !in eligibleMemberIdSet) continue

            val member = members[memberId] ?: continue
            val weight = weightMap[memberId] ?: BigDecimal.ZERO

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
                        memberId = memberId,
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
            participantCount = allMemberIds.size
        )
    }
}
