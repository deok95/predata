package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class RewardService(
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository
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
        val question = questionRepository.findById(questionId)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다.") }

        // 2. 총 베팅 금액에서 수수료 계산
        val totalBetAmount = question.totalBetPool
        val totalFee = (totalBetAmount * FEE_PERCENTAGE).toLong()
        val rewardPool = (totalFee * REWARD_POOL_PERCENTAGE).toLong()

        // 3. 투표한 티케터들 조회
        val voters = activityRepository.findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)

        if (voters.isEmpty()) {
            return RewardDistributionResult(
                questionId = questionId,
                totalRewardPool = rewardPool,
                totalVoters = 0,
                averageReward = 0L,
                distributedRewards = emptyList()
            )
        }

        // 4. 배치 조회: N+1 → 1회 조회
        val memberIds = voters.map { it.memberId }.distinct()
        val membersMap = memberRepository.findAllByIdIn(memberIds).associateBy { it.id!! }

        // 5. 티어 가중치 합계 계산
        var totalWeight = BigDecimal.ZERO
        val voterWeights = mutableMapOf<Long, BigDecimal>()

        voters.forEach { vote ->
            val member = membersMap[vote.memberId]
            if (member != null) {
                val weight = member.tierWeight
                voterWeights[vote.memberId] = weight
                totalWeight = totalWeight.add(weight)
            }
        }

        // 6. 가중치에 따라 보상 분배
        val distributedRewards = mutableListOf<VoterReward>()
        var totalDistributed = 0L
        val updatedMembers = mutableListOf<com.predata.backend.domain.Member>()

        voterWeights.forEach { (memberId, weight) ->
            val member = membersMap[memberId]
            if (member != null) {
                // 가중치 비율에 따른 보상 계산
                val rewardAmount = BigDecimal(rewardPool)
                    .multiply(weight)
                    .divide(totalWeight, 0, RoundingMode.DOWN)
                    .toLong()

                member.usdcBalance = member.usdcBalance.add(BigDecimal(rewardAmount))
                updatedMembers.add(member)

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

        // 7. 일괄 저장
        memberRepository.saveAll(updatedMembers)

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
        // TODO: 보상 내역을 별도 테이블에 저장하면 더 정확하지만,
        // 일단 현재 포인트 잔액으로 표시
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        // 투표 횟수 조회
        val votes = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.VOTE)

        return TotalRewardResponse(
            memberId = memberId,
            currentBalance = member.usdcBalance.toLong(),
            totalVotes = votes.size,
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
    val tier: String,
    val tierWeight: Double,
    val estimatedRewardPerVote: Long
)
