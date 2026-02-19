package com.predata.backend.service

import com.predata.backend.config.RiskConfig
import com.predata.backend.repository.PositionRepository
import com.predata.backend.repository.TradeRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 리스크 가드 서비스
 * - 주문 생성 시 리스크 한도를 체크
 */
@Service
class RiskGuardService(
    private val positionRepository: PositionRepository,
    private val tradeRepository: TradeRepository,
    private val config: RiskConfig,
    @Value("\${app.market-maker.member-id}") private val marketMakerMemberId: Long
) {

    /**
     * 포지션 한도 체크
     * - 해당 question에 대한 회원의 총 포지션 (YES + NO)이 한도를 초과하는지 확인
     */
    fun checkPositionLimit(
        memberId: Long,
        questionId: Long,
        additionalQty: BigDecimal
    ): RiskCheckResult {
        // Market maker is a system account intended to provide liquidity and can exceed normal risk limits.
        if (memberId == marketMakerMemberId) {
            return RiskCheckResult(passed = true)
        }

        val currentPositions = positionRepository.findByMemberIdAndQuestionId(memberId, questionId)
        val currentTotal = currentPositions.sumOf { it.quantity }
        val newTotal = currentTotal.add(additionalQty)

        return if (newTotal > BigDecimal(config.maxPositionPerMarket)) {
            RiskCheckResult(
                passed = false,
                message = "Position limit exceeded"
            )
        } else {
            RiskCheckResult(passed = true)
        }
    }

    /**
     * 주문 금액 한도 체크
     */
    fun checkOrderValueLimit(orderValue: Double): RiskCheckResult {
        return if (orderValue > config.maxOrderValue) {
            RiskCheckResult(
                passed = false,
                message = "Order value limit exceeded"
            )
        } else {
            RiskCheckResult(passed = true)
        }
    }

    /**
     * 서킷 브레이커 체크
     * - 1분 내 동일 question에 100건 이상 거래 발생 시 30초 쿨다운
     */
    fun checkCircuitBreaker(questionId: Long): RiskCheckResult {
        val windowStart = LocalDateTime.now()
            .minusSeconds(config.circuitBreaker.timeWindowSeconds.toLong())
        val recentTradeCount = tradeRepository.countByQuestionIdAndExecutedAtAfter(
            questionId,
            windowStart
        )

        return if (recentTradeCount >= config.circuitBreaker.tradeCountThreshold) {
            RiskCheckResult(
                passed = false,
                message = "Temporarily suspended due to high trading volume (will resume in ${config.circuitBreaker.cooldownSeconds} seconds)"
            )
        } else {
            RiskCheckResult(passed = true)
        }
    }
}

/**
 * 리스크 체크 결과
 */
data class RiskCheckResult(
    val passed: Boolean,
    val message: String? = null
)
