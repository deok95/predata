package com.predata.backend.service.settlement.adapters

import com.predata.backend.domain.Question
import com.predata.backend.domain.MarketType

/**
 * 정산 결과를 결정하는 어댑터 인터페이스
 * 각 시장 타입(스포츠, 주가, 의견 등)별로 구현체 제공
 */
interface ResolutionAdapter {
    /**
     * 질문에 대한 정산 결과를 결정
     */
    fun resolve(question: Question): ResolutionResult

    /**
     * 이 어댑터가 지원하는 시장 타입인지 확인
     */
    fun supports(marketType: MarketType): Boolean

    /**
     * 특정 source 스키마를 지원하는지 확인
     * 기본값은 false이며, source 기반 분기 어댑터만 override 한다.
     */
    fun supportsSource(resolutionSource: String?): Boolean = false
}
