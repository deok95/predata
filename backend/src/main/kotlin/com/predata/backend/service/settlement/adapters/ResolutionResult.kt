package com.predata.backend.service.settlement.adapters

import com.predata.backend.domain.FinalResult

/**
 * 정산 어댑터가 반환하는 결과
 */
data class ResolutionResult(
    val result: FinalResult,           // YES, NO, PENDING
    val sourcePayload: String,          // 외부 API 응답 원본 JSON 또는 데이터
    val sourceUrl: String? = null,      // 외부 데이터 소스 URL
    val confidence: Double = 1.0        // 신뢰도 (0.0 ~ 1.0)
)
