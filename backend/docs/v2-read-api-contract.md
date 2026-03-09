# PRE(D)ATA V2 Read API Contract

이 문서는 프론트 연동용 조회 응답 계약을 고정한다. 시간은 UTC(ISO-8601) 문자열 기준이다.

## 1) 질문 목록
- `GET /api/questions?page={number}&size={number}&sortBy={field}&sortDir={asc|desc}`

응답 예시:
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "title": "시장은 금요일 종가가 전일 대비 상승할 거라고 생각할까요?",
      "category": "FINANCE",
      "status": "BETTING",
      "type": "OPINION",
      "executionModel": "AMM_FPMM",
      "finalResult": "PENDING",
      "totalBetPool": 12450,
      "yesBetPool": 6870,
      "noBetPool": 5580,
      "yesPercentage": 55.18,
      "noPercentage": 44.82,
      "sourceUrl": null,
      "disputeDeadline": null,
      "votingEndAt": "2026-02-23T06:00:00",
      "bettingStartAt": "2026-02-23T06:05:00",
      "bettingEndAt": "2026-02-26T06:05:00",
      "expiredAt": "2026-02-26T06:05:00",
      "createdAt": "2026-02-22T23:10:00",
      "viewCount": 138,
      "matchId": null
    }
  ]
}
```

## 2) 질문 상세
- `GET /api/questions/{id}`

응답 스키마는 질문 목록의 단일 항목과 동일하다.

추가 규칙 (투트랙):
- `settlementMode`가 `VOTE_RESULT`인 질문은 `bettingEndAt` 이전에 yes/no 상세를 공개하지 않는다.
- `revealWindowEndAt`은 `bettingEndAt`과 동일한 의미로 사용한다(= reveal 종료 시점).
- `voteVisibility` 필드가 있을 경우 UI는 `REVEALED`일 때만 yes/no 비율을 표시한다.

## 3) 투표 가능 상태
- `GET /api/votes/status/{questionId}`

응답 예시:
```json
{
  "success": true,
  "data": {
    "questionId": 101,
    "canVote": true,
    "alreadyVoted": false,
    "remainingDailyVotes": 4,
    "reason": null
  }
}
```

`reason` 허용값:
- `QUESTION_NOT_FOUND`
- `VOTING_CLOSED`
- `ALREADY_VOTED`
- `DAILY_LIMIT_EXCEEDED`

## 4) 내 포트폴리오 요약
- `GET /api/portfolio/summary`

응답 예시:
```json
{
  "success": true,
  "data": {
    "memberId": 12,
    "totalInvested": 1200,
    "totalReturns": 1460,
    "netProfit": 260,
    "unrealizedValue": 85,
    "currentBalance": 2847,
    "winRate": 57.4,
    "totalBets": 47,
    "openBets": 9,
    "settledBets": 38,
    "roi": 21.67
  }
}
```

## 5) 내 오픈 포지션
- `GET /api/portfolio/positions?page={number}&size={number}&sortBy={placedAt|betAmount|estimatedProfitLoss}&sortDir={asc|desc}`

응답 예시:
```json
{
  "success": true,
  "data": [
    {
      "activityId": 9991,
      "questionId": 101,
      "questionTitle": "시장은 금요일 종가가 전일 대비 상승할 거라고 생각할까요?",
      "category": "FINANCE",
      "choice": "YES",
      "betAmount": 100,
      "currentYesPercentage": 63.2,
      "currentNoPercentage": 36.8,
      "estimatedPayout": 152,
      "estimatedProfitLoss": 52,
      "expiresAt": "2026-02-26T06:05:00",
      "placedAt": "2026-02-24T02:10:00"
    }
  ]
}
```

## 6) 정산 이력
- `GET /api/settlements/history/me`

응답 예시:
```json
{
  "success": true,
  "data": [
    {
      "questionId": 101,
      "questionTitle": "시장은 금요일 종가가 전일 대비 상승할 거라고 생각할까요?",
      "myChoice": "YES",
      "finalResult": "YES",
      "betAmount": 100,
      "payout": 182,
      "profit": 82,
      "isWinner": true
    }
  ]
}
```

## 7) 운영자 마켓 배치 조회
- `GET /api/admin/markets/batches?from={utcIso}&to={utcIso}`
- `GET /api/admin/markets/batches/{batchId}/candidates`
- `GET /api/admin/markets/batches/{batchId}/summary`

배치/후보 응답은 서버 DTO(`MarketBatchDtos.kt`)를 계약 기준으로 사용한다.

## 8) 마이페이지 요약
- `GET /api/members/me/dashboard`

응답 예시:
```json
{
  "success": true,
  "data": {
    "memberId": 12,
    "followers": 234,
    "following": 89,
    "questionsCreated": 12,
    "totalVotes": 156,
    "voteCredits": 3,
    "creatorEarnings": 142.30,
    "memberSince": "2026-01-05T10:11:12"
  }
}
```

## 9) 내가 생성한 질문 목록
- `GET /api/questions/me/created?page={number}&size={number}`

응답 예시:
```json
{
  "success": true,
  "data": [
    {
      "questionId": 701,
      "title": "Will CPI YoY be below 3.0% in March 2026?",
      "category": "economy",
      "status": "BETTING",
      "totalVotes": 842,
      "earnings": 12.54,
      "createdAt": "2026-02-20T04:30:00"
    }
  ]
}
```
