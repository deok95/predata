package com.predata.settlement.service

import com.predata.common.client.BettingClient
import com.predata.common.client.MemberClient
import com.predata.common.client.QuestionClient
import com.predata.common.dto.PointsRequest
import com.predata.settlement.domain.Reward
import com.predata.settlement.repository.RewardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class RewardService(
    private val memberClient: MemberClient,
    private val bettingClient: BettingClient,
    private val questionClient: QuestionClient,
    private val rewardRepository: RewardRepository
) {

    companion object {
        const val FEE_PERCENTAGE = 0.01 // 베팅 수수료 1%
        const val REWARD_POOL_PERCENTAGE = 0.50 // 수수료의 50%를 티케터 보상에 배분
    }

    /**
     * 질문 정산 시 티케터 보상 분배
     */
    @Transactional
    fun distributeRewards(questionId: Long): RewardDistributionResult {
        // 1. 질문 조회
        val questionResponse = questionClient.getQuestion(questionId)
        val question = questionResponse.data ?: throw IllegalArgumentException("질문을 찾을 수 없습니다.")

        // 2. 총 베팅 금액에서 수수료 계산
        val totalBetAmount = question.totalBetPool
        val totalFee = (totalBetAmount * FEE_PERCENTAGE).toLong()
        val rewardPool = (totalFee * REWARD_POOL_PERCENTAGE).toLong()

        // 3. 투표한 티케터들 조회
        val votersResponse = bettingClient.getActivitiesByQuestion(questionId, "VOTE")
        val voters = votersResponse.data ?: emptyList()

        if (voters.isEmpty()) {
            return RewardDistributionResult(
                questionId = questionId,
                totalRewardPool = rewardPool,
                totalVoters = 0,
                averageReward = 0L,
                distributedRewards = emptyList()
            )
        }

        // 4. 티어 가중치 합계 계산
        var totalWeight = BigDecimal.ZERO
        val voterWeights = mutableMapOf<Long, BigDecimal>()

        voters.forEach { vote ->
            val memberResponse = memberClient.getMember(vote.memberId)
            val member = memberResponse.data
            if (member != null) {
                voterWeights[vote.memberId] = member.tierWeight
                totalWeight = totalWeight.add(member.tierWeight)
            }
        }

        // 5. 가중치에 따라 보상 분배
        val distributedRewards = mutableListOf<VoterReward>()
        var totalDistributed = 0L

        voterWeights.forEach { (memberId, weight) ->
            val memberResponse = memberClient.getMember(memberId)
            val member = memberResponse.data
            if (member != null) {
                // 가중치 비율에 따른 보상 계산
                val rewardAmount = BigDecimal(rewardPool)
                    .multiply(weight)
                    .divide(totalWeight, 0, RoundingMode.DOWN)
                    .toLong()

                // 포인트 지급 (Member Service에 요청)
                memberClient.addPoints(memberId, PointsRequest(rewardAmount))

                // 보상 내역 저장
                val reward = Reward(
                    memberId = memberId,
                    questionId = questionId,
                    amount = rewardAmount,
                    rewardType = "VOTER_REWARD"
                )
                rewardRepository.save(reward)

                distributedRewards.add(
                    VoterReward(
                        memberId = memberId,
                        tier = member.tier,
                        tierWeight = weight.toDouble(),
                        rewardAmount = rewardAmount
                    )
                )

                totalDistributed += rewardAmount
            }
        }

        return RewardDistributionResult(
            questionId = questionId,
            totalRewardPool = rewardPool,
            totalVoters = voters.size,
            averageReward = if (voters.isNotEmpty()) totalDistributed / voters.size else 0L,
            distributedRewards = distributedRewards
        )
    }

    /**
     * 사용자별 누적 보상 조회
     */
    @Transactional(readOnly = true)
    fun getTotalRewards(memberId: Long): TotalRewardResponse {
        // Member 조회
        val memberResponse = memberClient.getMember(memberId)
        val member = memberResponse.data ?: throw IllegalArgumentException("회원을 찾을 수 없습니다.")

        // 투표 횟수 조회
        val votesResponse = bettingClient.getActivitiesByMember(memberId)
        val votes = votesResponse.data?.filter { it.activityType == "VOTE" } ?: emptyList()

        // 보상 총액 조회
        val totalVoterRewards = rewardRepository.getTotalRewardsByType(memberId, "VOTER_REWARD") ?: 0L

        return TotalRewardResponse(
            memberId = memberId,
            currentBalance = member.pointBalance,
            totalVotes = votes.size,
            totalVoterRewards = totalVoterRewards,
            tier = member.tier,
            tierWeight = member.tierWeight.toDouble(),
            estimatedRewardPerVote = calculateEstimatedReward(member.tierWeight)
        )
    }

    /**
     * 티어별 예상 보상 계산 (참고용)
     */
    private fun calculateEstimatedReward(tierWeight: BigDecimal): Long {
        // 평균적으로 1,000,000 포인트 베팅 풀이라고 가정
        val avgBetPool = 1000000L
        val avgFee = (avgBetPool * FEE_PERCENTAGE).toLong()
        val avgRewardPool = (avgFee * REWARD_POOL_PERCENTAGE).toLong()

        // 평균 100명 투표, 평균 티어 가중치 1.0이라고 가정
        val avgVoters = 100
        val avgWeight = BigDecimal("1.00")

        return BigDecimal(avgRewardPool)
            .multiply(tierWeight)
            .divide(avgWeight.multiply(BigDecimal(avgVoters)), 0, RoundingMode.DOWN)
            .toLong()
    }
}

// ===== DTOs =====

data class RewardDistributionResult(
    val questionId: Long,
    val totalRewardPool: Long,
    val totalVoters: Int,
    val averageReward: Long,
    val distributedRewards: List<VoterReward>
)

data class VoterReward(
    val memberId: Long,
    val tier: String,
    val tierWeight: Double,
    val rewardAmount: Long
)

data class TotalRewardResponse(
    val memberId: Long,
    val currentBalance: Long,
    val totalVotes: Int,
    val totalVoterRewards: Long,
    val tier: String,
    val tierWeight: Double,
    val estimatedRewardPerVote: Long
)
