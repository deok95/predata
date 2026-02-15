package com.predata.backend.service.settlement

import com.predata.backend.domain.QuestionType
import org.springframework.stereotype.Component

@Component
class SettlementPolicyFactory(
    private val policies: List<SettlementPolicy>
) {
    fun getPolicy(questionType: QuestionType): SettlementPolicy {
        return policies.firstOrNull { it.supports(questionType) }
            ?: error("지원되지 않는 정산 정책 타입입니다: $questionType")
    }
}
