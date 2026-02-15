package com.predata.backend.service.settlement.adapters

import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import org.springframework.stereotype.Component

/**
 * 시장 타입에 따라 적절한 정산 어댑터를 제공하는 레지스트리
 */
@Component
class ResolutionAdapterRegistry(
    private val adapters: List<ResolutionAdapter>
) {
    /**
     * 질문에 맞는 어댑터를 찾아 정산 결과를 반환
     */
    fun resolve(question: Question): ResolutionResult {
        val adapter = getAdapter(question.marketType, question.resolutionSource)
            ?: throw IllegalStateException("${question.marketType} 타입에 대한 정산 어댑터를 찾을 수 없습니다.")

        return adapter.resolve(question)
    }

    /**
     * 시장 타입과 resolutionSource에 맞는 어댑터 반환
     */
    private fun getAdapter(marketType: MarketType, resolutionSource: String?): ResolutionAdapter? {
        // resolutionSource 기반 특정 어댑터 선택 (예: stock://)
        if (resolutionSource != null) {
            adapters.forEach { adapter ->
                if (adapter is StockResolutionAdapter && adapter.supportsSource(resolutionSource)) {
                    return adapter
                }
            }
        }

        // marketType 기반 일반 어댑터 선택
        return adapters.firstOrNull { it.supports(marketType) }
    }
}
