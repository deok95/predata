package com.predata.backend.service

import com.predata.backend.dto.SellBetRequest
import com.predata.backend.dto.SellBetResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BetSellService {

    /**
     * 베팅 판매 실행 (비활성화됨)
     * - AMM 판매는 더 이상 지원되지 않음
     * - 오더북 반대 주문으로 포지션을 청산해야 함
     */
    @Transactional
    fun sellBet(memberId: Long, request: SellBetRequest, clientIp: String? = null): SellBetResponse {
        throw IllegalStateException("AMM selling is no longer supported. Please liquidate positions through opposite orders in the market.")
    }
}
