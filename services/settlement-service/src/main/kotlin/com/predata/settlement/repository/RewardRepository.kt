package com.predata.settlement.repository

import com.predata.settlement.domain.Reward
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RewardRepository : JpaRepository<Reward, Long> {
    fun findByMemberId(memberId: Long): List<Reward>
    fun findByQuestionId(questionId: Long): List<Reward>

    @Query("SELECT SUM(r.amount) FROM Reward r WHERE r.memberId = :memberId AND r.rewardType = :rewardType")
    fun getTotalRewardsByType(memberId: Long, rewardType: String): Long?
}
