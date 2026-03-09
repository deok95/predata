# Vote Two-Track API Schema Changes

기준일: 2026-02-24 (UTC)  
목적: 투트랙 설계 반영 시 프론트/백엔드 동시 수정 포인트를 고정한다.

## 1. 질문 생성/조회

### 1.1 Draft Submit (Write)
- Endpoint: `POST /api/questions/drafts/{draftId}/submit`
- 변경:
- `settlementMode` 필수 필드화
- 허용값: `OBJECTIVE_RULE | VOTE_RESULT`

### 1.2 Question Detail (Read)
- Endpoint: `GET /api/questions/{id}`
- 추가/변경:
- `settlementMode: "OBJECTIVE_RULE" | "VOTE_RESULT"`
- `voteVisibility: "OPEN" | "HIDDEN_UNTIL_REVEAL" | "REVEALED"` (신규)
- `revealWindowEndAt` 의미 재정의:
- 기존: `votingEndAt + 24h` (사용하지 않음)
- 현재: `bettingEndAt`와 동일 (reveal 종료 시점)
- `voteSummary` 공개 규칙 분기:
- OBJECTIVE_RULE: `yesCount/noCount/totalCount`
- VOTE_RESULT(reveal 전): `totalCount`만 공개, `yesCount/noCount` 숨김

## 2. 투표 Write 경로 분기

### 2.1 OBJECTIVE_RULE 질문
- 허용: `POST /api/votes`
- 요청:
```json
{
  "questionId": 123,
  "choice": "YES"
}
```

### 2.2 VOTE_RESULT 질문
- 허용: `POST /api/votes/commit`, `POST /api/votes/reveal`

`POST /api/votes/commit`
```json
{
  "questionId": 123,
  "commitHash": "hex_sha256"
}
```

`POST /api/votes/reveal`
```json
{
  "questionId": 123,
  "choice": "YES",
  "salt": "random-string"
}
```

### 2.3 모드 불일치 호출 오류 코드 (권장)
- `VOTE_MODE_MISMATCH`
- 예: VOTE_RESULT 질문에 `POST /api/votes` 호출 시 409 또는 422

## 3. 투표 상태 조회

### 3.1 Vote Status
- Endpoint: `GET /api/votes/status/{questionId}`
- 응답 필드 추가:
- `settlementMode`
- `voteVisibility`
- `votingPhase` (VOTE_RESULT 전용)

예시:
```json
{
  "canVote": true,
  "alreadyVoted": false,
  "remainingDailyVotes": 3,
  "settlementMode": "VOTE_RESULT",
  "voteVisibility": "HIDDEN_UNTIL_REVEAL",
  "votingPhase": "COMMIT_OPEN"
}
```

## 4. 정산 API

### 4.1 Auto Settlement
- Endpoint: `POST /api/admin/settlements/questions/{id}/settle-auto`
- 변경:
- 내부에서 `settlementMode` 기반 분기 수행

### 4.2 Manual Settlement
- Endpoint: `POST /api/admin/settlements/questions/{id}/settle`
- OBJECTIVE_RULE: 외부 기준 입력 허용
- VOTE_RESULT: 수동 finalResult 입력 제한 권장(운영 override만 허용)

## 5. 프론트 수정 체크리스트

- `QuestionSubmitView`:
- `settlementMode` 값 강제 (`OBJECTIVE_RULE | VOTE_RESULT`)
- `VotePage/VoteDetail`:
- 질문 `settlementMode` 확인 후 write endpoint 분기
- `VOTE_RESULT` + reveal 전 yes/no UI 숨김
- `api.js`:
- `voteApi.vote`와 `voteApi.commit/reveal` 동시 유지
- 모드 불일치 에러 핸들링 추가

## 6. QA 계약 항목

- OBJECTIVE_RULE 질문:
- `POST /api/votes` 성공
- commit/reveal 실패(모드 불일치)
- VOTE_RESULT 질문:
- commit/reveal 성공
- `POST /api/votes` 실패(모드 불일치)
- reveal 전 상세 조회에서 `yes/no` 미노출
- reveal 후 상세 조회에서 `yes/no` 노출
