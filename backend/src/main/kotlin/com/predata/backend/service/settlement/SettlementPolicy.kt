package com.predata.backend.service.settlement

import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.QuestionType

interface SettlementPolicy {
    fun supports(questionType: QuestionType): Boolean
    fun determineWinningChoice(finalResult: FinalResult): Choice
    fun calculatePayout(betAmount: Long, totalPool: Long, winningPool: Long): Long
}
