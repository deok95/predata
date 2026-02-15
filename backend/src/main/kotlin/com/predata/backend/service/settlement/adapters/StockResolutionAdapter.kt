package com.predata.backend.service.settlement.adapters

import com.predata.backend.domain.Question
import com.predata.backend.domain.MarketType
import org.springframework.stereotype.Component

/**
 * 주가 결과 정산 어댑터 (stub)
 * TODO: 주가 API 연동 구현 예정
 */
@Component
class StockResolutionAdapter : ResolutionAdapter {

    override fun supports(marketType: MarketType): Boolean {
        // resolutionSource가 "stock://" 으로 시작할 때만 지원
        return false // Registry에서 직접 체크하므로 여기서는 false
    }

    override fun resolve(question: Question): ResolutionResult {
        // TODO: 주가 API 연동 구현
        // 1. resolutionSource에서 종목 코드 파싱 (예: stock://AAPL)
        // 2. 주가 API 호출하여 가격 조회
        // 3. resolutionRule 파싱하여 YES/NO 판정
        // 예: resolutionRule = "애플 주가가 $200 이상일까?" → price >= 200

        throw UnsupportedOperationException("Stock adapter not yet implemented")
    }
}
