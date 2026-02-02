package com.predata.settlement.controller

import com.predata.common.domain.FinalResult
import com.predata.common.dto.ApiResponse
import com.predata.settlement.service.SettlementHistoryItem
import com.predata.settlement.service.SettlementResult
import com.predata.settlement.service.SettlementService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/settlements")
@CrossOrigin(origins = ["http://localhost:3000"])
class SettlementController(
    private val settlementService: SettlementService
) {

    @PostMapping("/{questionId}/settle")
    fun settleQuestion(
        @PathVariable questionId: Long,
        @RequestBody request: SettleRequest
    ): ApiResponse<SettlementResult> {
        val result = settlementService.settleQuestion(questionId, FinalResult.valueOf(request.finalResult))
        return ApiResponse(success = true, data = result)
    }

    @GetMapping("/history/{memberId}")
    fun getSettlementHistory(@PathVariable memberId: Long): ApiResponse<List<SettlementHistoryItem>> {
        val history = settlementService.getSettlementHistory(memberId)
        return ApiResponse(success = true, data = history)
    }
}

data class SettleRequest(
    val finalResult: String
)
