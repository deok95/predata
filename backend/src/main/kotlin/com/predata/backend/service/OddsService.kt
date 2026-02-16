package com.predata.backend.service

import com.predata.backend.dto.OddsResult
import org.springframework.stereotype.Service

@Service
class OddsService {
    /**
     * 배당률 계산 로직 (프론트엔드 engine.ts와 동일)
     */
    fun calculateOdds(poolYes: Double, poolNo: Double, subsidy: Double): OddsResult {
        val effectiveYes = poolYes + (subsidy / 2.0)
        val effectiveNo = poolNo + (subsidy / 2.0)
        val totalPool = effectiveYes + effectiveNo

        if (totalPool == 0.0) {
            return OddsResult(
                yesOdds = "2.00",
                noOdds = "2.00",
                yesPrice = "0.50",
                noPrice = "0.50"
            )
        }

        // 가격의 합이 1.01이 되도록 수수료 1% 적용
        val yesPrice = (effectiveYes / totalPool) * 1.01
        val noPrice = (effectiveNo / totalPool) * 1.01

        return OddsResult(
            yesOdds = String.format("%.2f", 1.0 / yesPrice),
            noOdds = String.format("%.2f", 1.0 / noPrice),
            yesPrice = String.format("%.2f", yesPrice),
            noPrice = String.format("%.2f", noPrice)
        )
    }
}
