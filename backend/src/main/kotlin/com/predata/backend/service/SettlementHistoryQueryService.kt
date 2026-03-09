package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service

@Service
class SettlementHistoryQueryService(
    private val activityRepository: ActivityRepository,
    private val questionRepository: QuestionRepository,
) {
    fun getSettlementHistory(memberId: Long): List<SettlementHistoryItem> {
        val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)
        val questionIds = bets.map { it.questionId }.distinct()
        val questionsMap = questionRepository.findAllById(questionIds).associateBy { it.id!! }

        return bets.mapNotNull { bet ->
            val question = questionsMap[bet.questionId]
            if (question == null || question.status != QuestionStatus.SETTLED) return@mapNotNull null

            val winningChoice = when (question.finalResult) {
                FinalResult.YES -> Choice.YES
                FinalResult.NO -> Choice.NO
                FinalResult.PENDING -> return@mapNotNull null
            }
            val isWinner = bet.choice == winningChoice
            val payout = if (isWinner) bet.amount else 0L

            SettlementHistoryItem(
                questionId = question.id ?: 0,
                questionTitle = question.title,
                myChoice = bet.choice.name,
                finalResult = question.finalResult.name,
                betAmount = bet.amount,
                payout = payout,
                profit = payout - bet.amount,
                isWinner = isWinner
            )
        }
    }
}
