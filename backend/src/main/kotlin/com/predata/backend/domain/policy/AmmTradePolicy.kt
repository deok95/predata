package com.predata.backend.domain.policy

import com.predata.backend.domain.ExecutionModel
import com.predata.backend.domain.PoolStatus
import com.predata.backend.domain.QuestionStatus
import java.math.BigDecimal

object AmmTradePolicy {
    fun ensureAmmQuestion(executionModel: ExecutionModel) {
        require(executionModel == ExecutionModel.AMM_FPMM) {
            "해당 질문은 AMM 실행 모델이 아닙니다."
        }
    }

    fun ensureBettingStatus(status: QuestionStatus) {
        require(status == QuestionStatus.BETTING) {
            "베팅 기간이 아닙니다. 현재 상태: $status"
        }
    }

    fun ensurePoolActive(status: PoolStatus) {
        require(status == PoolStatus.ACTIVE) {
            "마켓 풀이 활성화되지 않았습니다. 현재 상태: $status"
        }
    }

    fun ensureBuyInput(usdcIn: BigDecimal?, minAmount: BigDecimal) {
        val amount = usdcIn ?: throw IllegalArgumentException("usdcIn is required for BUY.")
        require(amount >= minAmount) {
            "최소 거래 금액은 $minAmount USDC입니다."
        }
    }

    fun ensureSellInput(sharesIn: BigDecimal?, minAmount: BigDecimal) {
        val amount = sharesIn ?: throw IllegalArgumentException("sharesIn is required for SELL.")
        require(amount >= minAmount) {
            "최소 거래 금액은 $minAmount shares입니다."
        }
    }

    fun ensureUsdcBalance(available: BigDecimal, requiredAmount: BigDecimal) {
        require(available >= requiredAmount) {
            "USDC 잔액이 부족합니다. 보유: $available, 필요: $requiredAmount"
        }
    }

    fun ensureOwnedShares(ownedShares: BigDecimal, requiredShares: BigDecimal) {
        require(ownedShares >= requiredShares) {
            "shares 잔액이 부족합니다. 보유: $ownedShares, 필요: $requiredShares"
        }
    }

    fun ensureMinSharesOut(actualSharesOut: BigDecimal, minSharesOut: BigDecimal) {
        require(actualSharesOut >= minSharesOut) {
            "슬리피지 초과: 예상 최소 $minSharesOut shares, 실제 $actualSharesOut shares"
        }
    }

    fun ensureMinUsdcOut(actualUsdcOut: BigDecimal, minUsdcOut: BigDecimal) {
        require(actualUsdcOut >= minUsdcOut) {
            "슬리피지 초과: 예상 최소 $minUsdcOut USDC, 실제 $actualUsdcOut USDC"
        }
    }

    fun ensureSeedRequest(seedUsdc: BigDecimal, feeRate: BigDecimal, zero: BigDecimal, one: BigDecimal) {
        require(seedUsdc > zero) {
            "시드 금액은 0보다 커야 합니다."
        }
        require(feeRate >= zero && feeRate < one) {
            "수수료율은 [0, 1) 범위여야 합니다."
        }
    }

    fun ensureSeedableQuestion(status: QuestionStatus) {
        require(status != QuestionStatus.SETTLED && status != QuestionStatus.CANCELLED) {
            "정산 완료되거나 취소된 질문에는 풀을 생성할 수 없습니다."
        }
    }
}
