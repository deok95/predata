package com.predata.backend.service.settlement.adapters

import com.predata.backend.domain.Question
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Choice
import com.predata.backend.repository.ActivityRepository
import org.springframework.stereotype.Component

/**
 * 의견 기반 정산 어댑터
 * 투표 결과를 기반으로 정산 (YES 투표 > NO 투표 → result=YES)
 */
@Component
class OpinionResolutionAdapter(
    private val activityRepository: ActivityRepository
) : ResolutionAdapter {

    override fun supports(marketType: MarketType): Boolean {
        return marketType == MarketType.OPINION
    }

    override fun resolve(question: Question): ResolutionResult {
        val questionId = question.id
            ?: throw IllegalArgumentException("Question ID가 없습니다.")

        // 투표 활동 조회
        val votes = activityRepository.findByQuestionId(questionId)
            .filter { it.activityType == com.predata.backend.domain.ActivityType.VOTE }

        val yesVotes = votes.count { it.choice == Choice.YES }
        val noVotes = votes.count { it.choice == Choice.NO }

        val result = if (yesVotes > noVotes) FinalResult.YES else FinalResult.NO

        val sourcePayload = """
            {
              "questionId": $questionId,
              "yesVotes": $yesVotes,
              "noVotes": $noVotes,
              "totalVotes": ${votes.size},
              "result": "${result.name}"
            }
        """.trimIndent()

        return ResolutionResult(
            result = result,
            sourcePayload = sourcePayload,
            sourceUrl = null,
            confidence = if (votes.isNotEmpty()) {
                // 투표 차이에 따른 신뢰도 계산
                val totalVotes = votes.size
                val winningVotes = maxOf(yesVotes, noVotes)
                winningVotes.toDouble() / totalVotes
            } else {
                0.0
            }
        )
    }
}
