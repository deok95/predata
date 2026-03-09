package com.predata.backend.service

import com.predata.backend.exception.SettlementCancellationDisabledException
import org.springframework.stereotype.Service

@Service
class SettlementCancellationService {
    fun cancelPendingSettlement(questionId: Long): SettlementResult {
        throw SettlementCancellationDisabledException(
            "탈중앙화 정책에 따라 정산 취소는 지원하지 않습니다. questionId=$questionId"
        )
    }
}
