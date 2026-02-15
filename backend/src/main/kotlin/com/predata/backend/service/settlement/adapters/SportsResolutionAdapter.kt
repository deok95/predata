package com.predata.backend.service.settlement.adapters

import com.predata.backend.domain.Question
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.FinalResult
import org.springframework.stereotype.Component

/**
 * 스포츠 결과 정산 어댑터
 * football-data.org API를 통해 경기 결과를 조회하여 정산
 */
@Component
class SportsResolutionAdapter : ResolutionAdapter {

    override fun supports(marketType: MarketType): Boolean {
        return marketType == MarketType.VERIFIABLE
    }

    override fun resolve(question: Question): ResolutionResult {
        // TODO: football-data.org API 연동 구현
        // 1. resolutionSource에서 경기 ID 파싱
        // 2. football-data.org API 호출하여 경기 결과 조회
        // 3. resolutionRule 파싱하여 YES/NO 판정 로직 적용
        // 예: resolutionRule = "맨체스터 유나이티드가 승리할까?" → homeTeam.score > awayTeam.score

        val resolutionSource = question.resolutionSource
            ?: throw IllegalArgumentException("스포츠 정산을 위한 resolutionSource가 없습니다.")

        // 현재는 stub 구현: sourcePayload에 더미 데이터 저장
        val dummyApiResponse = """
            {
              "match": {
                "id": "${resolutionSource}",
                "homeTeam": "팀A",
                "awayTeam": "팀B",
                "homeScore": 2,
                "awayScore": 1,
                "status": "FINISHED"
              }
            }
        """.trimIndent()

        // TODO: 실제 resolutionRule 파싱 및 결과 판정 로직 구현
        // 현재는 더미로 YES 반환
        val result = FinalResult.YES

        return ResolutionResult(
            result = result,
            sourcePayload = dummyApiResponse,
            sourceUrl = "https://api.football-data.org/v4/matches/${resolutionSource}",
            confidence = 1.0
        )
    }
}
