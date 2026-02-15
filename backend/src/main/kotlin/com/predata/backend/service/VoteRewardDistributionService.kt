package com.predata.backend.service

import com.predata.backend.domain.*
import com.predata.backend.repository.FeePoolLedgerRepository
import com.predata.backend.repository.FeePoolRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.RewardDistributionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 투표 리워드 분배 서비스
 * - 자동 분배 실행
 * - 부분 실패 시 재시도 지원
 * - idempotencyKey로 중복 지급 방지
 */
@Service
class VoteRewardDistributionService(
    private val voteRewardService: VoteRewardService,
    private val rewardDistributionRepository: RewardDistributionRepository,
    private val memberRepository: MemberRepository,
    private val feePoolLedgerRepository: FeePoolLedgerRepository,
    private val feePoolRepository: FeePoolRepository,
    private val feePoolService: FeePoolService,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(VoteRewardDistributionService::class.java)

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    /**
     * 보상 분배 실행
     * 1. calculateRewards()로 보상 계산
     * 2. 각 투표자에게 포인트 지급 (Member.pointBalance += rewardAmount)
     * 3. FeePoolLedger에 REWARD_DISTRIBUTED 기록
     * 4. 부분 실패 시: 성공분 커밋 + 실패분 재시도 큐 등록
     */
    @Transactional
    fun distributeRewards(questionId: Long): Map<String, Any> {
        logger.info("Starting reward distribution for questionId=$questionId")

        // 1. 보상 계산
        val summary = voteRewardService.calculateRewards(questionId)

        // 감사 로그: 보상 계산
        auditService.log(
            memberId = null,
            action = AuditAction.REWARD_CALCULATED,
            entityType = "VoteReward",
            entityId = questionId,
            detail = "보상 계산: questionId=$questionId, pool=${summary.totalRewardPool}, participants=${summary.participantCount}"
        )

        if (summary.individualRewards.isEmpty()) {
            logger.warn("No rewards to distribute for questionId=$questionId")
            return mapOf(
                "success" to true,
                "message" to "분배할 보상이 없습니다.",
                "distributed" to 0,
                "failed" to 0
            )
        }

        var successCount = 0
        var failCount = 0
        var actualDistributed = BigDecimal.ZERO
        val errors = mutableListOf<String>()

        // 2. 각 투표자에게 포인트 지급
        for (reward in summary.individualRewards) {
            try {
                // attempts 체크: 3회 이상 실패한 건은 skip
                val idempotencyKey = generateIdempotencyKey(questionId, reward.memberId)
                val existing = rewardDistributionRepository.findByIdempotencyKey(idempotencyKey)
                if (existing != null && existing.attempts >= MAX_RETRY_ATTEMPTS) {
                    logger.warn("Skipping distribution for memberId=${reward.memberId}: max attempts reached (${existing.attempts})")
                    failCount++
                    continue
                }

                val distributed = distributeSingleReward(questionId, reward.memberId, reward.rewardAmount)
                if (distributed) {
                    successCount++
                    actualDistributed = actualDistributed.add(reward.rewardAmount)
                } else {
                    failCount++
                }
            } catch (e: Exception) {
                logger.error("Failed to distribute reward to memberId=${reward.memberId}: ${e.message}", e)
                errors.add("memberId=${reward.memberId}: ${e.message}")
                failCount++
            }
        }

        // 3. FeePoolLedger에 REWARD_DISTRIBUTED 기록 (실제 성공한 분배액만)
        if (actualDistributed > BigDecimal.ZERO) {
            try {
                // FeePool 조회하여 실제 feePoolId 사용
                val feePool = feePoolRepository.findByQuestionId(questionId).orElse(null)
                if (feePool != null) {
                    val ledger = FeePoolLedger(
                        feePoolId = feePool.id!!,
                        action = FeePoolAction.REWARD_DISTRIBUTED,
                        amount = actualDistributed,
                        balance = summary.totalRewardPool.subtract(actualDistributed),
                        description = "리워드 분배 완료 - 성공: $successCount, 실패: $failCount"
                    )
                    feePoolLedgerRepository.save(ledger)
                }
            } catch (e: Exception) {
                logger.error("Failed to record fee pool ledger for questionId=$questionId: ${e.message}", e)
            }
        }

        // 4. 절사 잔여분 리저브로 할당
        if (summary.truncatedAmount > BigDecimal.ZERO) {
            try {
                feePoolService.allocateToReserve(questionId, summary.truncatedAmount)
            } catch (e: Exception) {
                logger.error("Failed to allocate truncated amount to reserve for questionId=$questionId: ${e.message}", e)
            }
        }

        logger.info("Reward distribution completed: questionId=$questionId, success=$successCount, failed=$failCount, truncated=${summary.truncatedAmount}")

        return mapOf(
            "success" to true,
            "message" to "보상 분배 완료",
            "totalRewards" to summary.individualRewards.size,
            "distributed" to successCount,
            "failed" to failCount,
            "totalAmount" to actualDistributed,
            "errors" to errors
        )
    }

    /**
     * 단일 보상 분배 (idempotency 보장)
     * - REQUIRES_NEW로 별도 트랜잭션 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun distributeSingleReward(questionId: Long, memberId: Long, amount: BigDecimal): Boolean {
        val idempotencyKey = generateIdempotencyKey(questionId, memberId)

        // 중복 지급 체크
        val existing = rewardDistributionRepository.findByIdempotencyKey(idempotencyKey)
        if (existing != null) {
            if (existing.status == RewardDistributionStatus.SUCCESS) {
                logger.debug("Reward already distributed: questionId=$questionId, memberId=$memberId")
                return true
            }
            // 실패했던 건은 재시도
            existing.attempts++
        }

        // RewardDistribution 레코드 생성
        val distribution = existing ?: RewardDistribution(
            questionId = questionId,
            memberId = memberId,
            amount = amount,
            status = RewardDistributionStatus.PENDING,
            idempotencyKey = idempotencyKey,
            attempts = 1
        )

        try {
            // Member의 pointBalance 증가
            val member = memberRepository.findById(memberId).orElseThrow {
                IllegalArgumentException("회원을 찾을 수 없습니다: memberId=$memberId")
            }

            member.pointBalance = member.pointBalance.add(amount)
            memberRepository.save(member)

            // 성공 처리
            distribution.status = RewardDistributionStatus.SUCCESS
            distribution.completedAt = LocalDateTime.now()
            distribution.errorMessage = null
            rewardDistributionRepository.save(distribution)

            logger.info("Reward distributed: questionId=$questionId, memberId=$memberId, amount=$amount")

            // 감사 로그: 보상 분배 성공
            auditService.log(
                memberId = memberId,
                action = AuditAction.REWARD_DISTRIBUTED,
                entityType = "RewardDistribution",
                entityId = distribution.id,
                detail = "보상 분배 성공: questionId=$questionId, amount=$amount"
            )

            return true

        } catch (e: Exception) {
            logger.error("Failed to distribute reward: questionId=$questionId, memberId=$memberId, error=${e.message}", e)

            // 실패 처리
            distribution.status = RewardDistributionStatus.FAILED
            distribution.errorMessage = e.message?.take(1000)
            rewardDistributionRepository.save(distribution)

            // 감사 로그: 보상 분배 실패
            auditService.log(
                memberId = memberId,
                action = AuditAction.REWARD_FAILED,
                entityType = "RewardDistribution",
                entityId = distribution.id,
                detail = "보상 분배 실패: questionId=$questionId, error=${e.message}"
            )

            return false
        }
    }

    /**
     * 실패한 분배 재시도
     * - 최대 3회까지 재시도
     */
    @Transactional
    fun retryFailedDistributions(questionId: Long): Map<String, Any> {
        logger.info("Retrying failed distributions for questionId=$questionId")

        val failedDistributions = rewardDistributionRepository.findByQuestionIdAndStatus(
            questionId,
            RewardDistributionStatus.FAILED
        ).filter { it.attempts < MAX_RETRY_ATTEMPTS }

        if (failedDistributions.isEmpty()) {
            return mapOf(
                "success" to true,
                "message" to "재시도할 실패 기록이 없습니다.",
                "retried" to 0,
                "succeeded" to 0,
                "failed" to 0
            )
        }

        var successCount = 0
        var failCount = 0

        for (distribution in failedDistributions) {
            try {
                val success = distributeSingleReward(
                    distribution.questionId,
                    distribution.memberId,
                    distribution.amount
                )
                if (success) {
                    successCount++
                } else {
                    failCount++
                }
            } catch (e: Exception) {
                logger.error("Retry failed for distributionId=${distribution.id}: ${e.message}", e)
                failCount++
            }
        }

        logger.info("Retry completed: questionId=$questionId, succeeded=$successCount, failed=$failCount")

        return mapOf(
            "success" to true,
            "message" to "재시도 완료",
            "retried" to failedDistributions.size,
            "succeeded" to successCount,
            "failed" to failCount
        )
    }

    /**
     * Idempotency key 생성
     * - 패턴: questionId_memberId_reward
     */
    private fun generateIdempotencyKey(questionId: Long, memberId: Long): String {
        return "${questionId}_${memberId}_reward"
    }
}
