package com.predata.backend.repository

import com.predata.backend.domain.RewardDistribution
import com.predata.backend.domain.RewardDistributionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RewardDistributionRepository : JpaRepository<RewardDistribution, Long> {
    /**
     * idempotencyKey로 분배 기록 조회
     */
    fun findByIdempotencyKey(idempotencyKey: String): RewardDistribution?

    /**
     * 질문 ID와 상태로 분배 기록 조회
     */
    fun findByQuestionIdAndStatus(questionId: Long, status: RewardDistributionStatus): List<RewardDistribution>

    /**
     * 질문 ID로 모든 분배 기록 조회
     */
    fun findByQuestionIdOrderByCreatedAtDesc(questionId: Long): List<RewardDistribution>

    /**
     * 재시도가 필요한 실패 기록 조회 (attempts < maxAttempts)
     */
    fun findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
        status: RewardDistributionStatus,
        maxAttempts: Int
    ): List<RewardDistribution>
}
