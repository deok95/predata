package com.predata.backend.service

import com.predata.backend.domain.VotingPhase
import com.predata.backend.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class SettlementPostProcessService(
    private val feePoolService: FeePoolService,
    private val memberRepository: MemberRepository,
    private val walletBalanceService: WalletBalanceService,
    private val transactionHistoryService: TransactionHistoryService,
    private val voteRewardDistributionService: VoteRewardDistributionService,
    private val questionLifecycleService: QuestionLifecycleService,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(SettlementPostProcessService::class.java)

    private val requiresNewTx = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun distributeCreatorShareNonBlocking(questionId: Long, creatorMemberId: Long?) {
        if (creatorMemberId == null) return

        try {
            requiresNewTx.execute {
                val creatorShare = feePoolService.distributeCreatorShare(questionId)
                if (creatorShare > BigDecimal.ZERO) {
                    val creator = memberRepository.findById(creatorMemberId).orElse(null)
                    if (creator != null) {
                        val wallet = walletBalanceService.credit(
                            memberId = creatorMemberId,
                            amount = creatorShare,
                            txType = "CREATOR_FEE_DISTRIBUTION",
                            referenceType = "QUESTION",
                            referenceId = questionId,
                            description = "질문 생성자 수수료 분배",
                        )
                        transactionHistoryService.record(
                            memberId = creatorMemberId,
                            type = "CREATOR_FEE_DISTRIBUTION",
                            amount = creatorShare,
                            balanceAfter = wallet.availableBalance,
                            description = "질문 생성자 수수료 분배 - Question #$questionId",
                            questionId = questionId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Creator fee distribution failed (non-blocking): questionId=$questionId, error=${e.message}", e)
        }
    }

    fun distributeVoterRewardsNonBlocking(questionId: Long): Long {
        return try {
            val rewardResult = requiresNewTx.execute {
                voteRewardDistributionService.distributeRewards(questionId)
            }
            val distributed = rewardResult?.get("totalAmount")
            if (distributed is BigDecimal) {
                distributed.setScale(0, RoundingMode.DOWN).toLong()
            } else {
                0L
            }.also {
                logger.info("Vote reward distribution completed: questionId=$questionId, result=$rewardResult")
            }
        } catch (e: Exception) {
            logger.error("Vote reward distribution failed (non-blocking): questionId=$questionId, error=${e.message}", e)
            0L
        }
    }

    fun transitionToRewardDistributedNonBlocking(questionId: Long, voterRewardsAmount: Long) {
        if (voterRewardsAmount <= 0) return

        try {
            requiresNewTx.execute {
                questionLifecycleService.transitionTo(questionId, VotingPhase.REWARD_DISTRIBUTED)
            }
        } catch (e: Exception) {
            logger.error("Phase transition to REWARD_DISTRIBUTED failed (non-blocking): questionId=$questionId", e)
        }
    }
}
