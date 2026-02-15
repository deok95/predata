package com.predata.backend.service

import com.predata.backend.domain.FeePool
import com.predata.backend.domain.FeePoolAction
import com.predata.backend.domain.FeePoolLedger
import com.predata.backend.repository.FeePoolLedgerRepository
import com.predata.backend.repository.FeePoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * 수수료 풀 서비스
 * - 수수료 수집 및 자동 분배 (30/60/10)
 * - 리워드 풀 잔액 관리
 */
@Service
class FeePoolService(
    private val feePoolRepository: FeePoolRepository,
    private val feePoolLedgerRepository: FeePoolLedgerRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(FeePoolService::class.java)

    companion object {
        // 분배 비율: 플랫폼 30%, 리워드 풀 60%, 리저브 10%
        private val PLATFORM_RATIO = BigDecimal("0.30")
        private val REWARD_POOL_RATIO = BigDecimal("0.60")
        private val RESERVE_RATIO = BigDecimal("0.10")
    }

    /**
     * 수수료 수집 및 자동 분배
     * - 총 수수료를 30/60/10 비율로 자동 분배
     * - FeePoolLedger에 FEE_COLLECTED 기록
     */
    @Transactional
    fun collectFee(questionId: Long, amount: BigDecimal): FeePool {
        require(amount > BigDecimal.ZERO) { "수수료는 0보다 커야 합니다." }

        // 1. FeePool 조회 또는 생성
        val feePool = feePoolRepository.findByQuestionId(questionId).orElseGet {
            val newPool = FeePool(questionId = questionId)
            feePoolRepository.save(newPool)
        }

        // 2. 분배 계산 (6자리 소수점, HALF_UP 반올림)
        val platformAmount = amount.multiply(PLATFORM_RATIO).setScale(6, RoundingMode.HALF_UP)
        val rewardPoolAmount = amount.multiply(REWARD_POOL_RATIO).setScale(6, RoundingMode.HALF_UP)
        val reserveAmount = amount.multiply(RESERVE_RATIO).setScale(6, RoundingMode.HALF_UP)

        // 3. FeePool 업데이트
        feePool.totalFees = feePool.totalFees.add(amount)
        feePool.platformShare = feePool.platformShare.add(platformAmount)
        feePool.rewardPoolShare = feePool.rewardPoolShare.add(rewardPoolAmount)
        feePool.reserveShare = feePool.reserveShare.add(reserveAmount)
        feePool.updatedAt = LocalDateTime.now()

        val updated = feePoolRepository.save(feePool)

        // 4. FeePoolLedger 기록
        val ledger = FeePoolLedger(
            feePoolId = updated.id!!,
            action = FeePoolAction.FEE_COLLECTED,
            amount = amount,
            balance = updated.totalFees,
            description = "수수료 수집 - 플랫폼: $platformAmount, 리워드: $rewardPoolAmount, 리저브: $reserveAmount"
        )
        feePoolLedgerRepository.save(ledger)

        logger.info("Fee collected: questionId=$questionId, amount=$amount, platform=$platformAmount, reward=$rewardPoolAmount, reserve=$reserveAmount")

        // 감사 로그 기록
        auditService.log(
            memberId = null,
            action = com.predata.backend.domain.AuditAction.FEE_COLLECTED,
            entityType = "FeePool",
            entityId = updated.id,
            detail = "수수료 수집: questionId=$questionId, amount=$amount"
        )

        return updated
    }

    /**
     * 분배 가능한 리워드 풀 잔액 조회
     * - 리워드 풀 총액 - 이미 분배된 금액
     */
    @Transactional(readOnly = true)
    fun getAvailableRewardPool(questionId: Long): BigDecimal {
        val feePool = feePoolRepository.findByQuestionId(questionId).orElse(null)
            ?: return BigDecimal.ZERO

        // 리워드 풀에서 이미 분배된 금액 계산
        val distributed = feePoolLedgerRepository.findByFeePoolIdAndActionOrderByCreatedAtDesc(
            feePool.id!!,
            FeePoolAction.REWARD_DISTRIBUTED
        ).sumOf { it.amount }

        val available = feePool.rewardPoolShare.subtract(distributed)
        return if (available > BigDecimal.ZERO) available else BigDecimal.ZERO
    }

    /**
     * 플랫폼 몫 인출
     * - 관리자 전용
     */
    @Transactional
    fun withdrawPlatformShare(questionId: Long): BigDecimal {
        val feePool = feePoolRepository.findByQuestionId(questionId).orElseThrow {
            IllegalArgumentException("수수료 풀을 찾을 수 없습니다.")
        }

        val availablePlatform = feePool.platformShare

        require(availablePlatform > BigDecimal.ZERO) { "인출 가능한 플랫폼 몫이 없습니다." }

        // 플랫폼 몫 초기화
        feePool.platformShare = BigDecimal.ZERO
        feePool.updatedAt = LocalDateTime.now()
        feePoolRepository.save(feePool)

        // 원장 기록
        val ledger = FeePoolLedger(
            feePoolId = feePool.id!!,
            action = FeePoolAction.PLATFORM_WITHDRAWN,
            amount = availablePlatform,
            balance = feePool.totalFees,
            description = "플랫폼 몫 인출"
        )
        feePoolLedgerRepository.save(ledger)

        logger.info("Platform share withdrawn: questionId=$questionId, amount=$availablePlatform")

        return availablePlatform
    }

    /**
     * 풀 현황 조회
     */
    @Transactional(readOnly = true)
    fun getPoolSummary(questionId: Long): Map<String, Any> {
        val feePool = feePoolRepository.findByQuestionId(questionId).orElse(null)
            ?: return mapOf(
                "success" to false,
                "message" to "수수료 풀을 찾을 수 없습니다."
            )

        val availableReward = getAvailableRewardPool(questionId)

        return mapOf(
            "success" to true,
            "questionId" to questionId,
            "totalFees" to feePool.totalFees,
            "platformShare" to feePool.platformShare,
            "rewardPoolShare" to feePool.rewardPoolShare,
            "availableRewardPool" to availableReward,
            "reserveShare" to feePool.reserveShare,
            "createdAt" to feePool.createdAt,
            "updatedAt" to feePool.updatedAt
        )
    }
}
