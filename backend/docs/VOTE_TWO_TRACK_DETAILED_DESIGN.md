# Vote Two-Track Detailed Design

기준일: 2026-02-24 (UTC)  
목표: 질문별 `settlementMode`에 따라 투표 공개 정책과 정산 소스를 분기한다.

## 1. 트랙 정의

### 1.1 OBJECTIVE_RULE 트랙 (오픈형)
- 투표 write: `POST /api/votes`
- 투표 공개: yes/no/total 공개 가능
- 베팅 결과 소스: 외부 기준(oracle/source URL)

### 1.2 VOTE_RESULT 트랙 (비하인드형)
- 투표 write: `POST /api/votes/commit`, `POST /api/votes/reveal`
- 투표 공개:
- commit/reveal 진행 중 yes/no 비공개
- reveal 종료 후 yes/no 공개
- 베팅 결과 소스: reveal 검증 완료 집계

## 2. 질문 생성 계약

- 질문 생성 submit payload에 `settlementMode` 필수
- 허용값:
- `OBJECTIVE_RULE`
- `VOTE_RESULT`
- 생성 후 사용자 변경 금지, ADMIN만 변경 가능
- 변경 시 감사로그 `before/after` 저장

## 3. 상태 머신

### 3.1 질문 상태 (공통)
- `VOTING -> BREAK -> BETTING -> SETTLED`

### 3.2 투표 내부 상태 (VOTE_RESULT 전용)
- `COMMIT_OPEN`
- `REVEAL_OPEN`
- `REVEAL_CLOSED`

### 3.3 강제 규칙
- `VOTE_RESULT` 질문은 `REVEAL_CLOSED` 전 결과 확정 금지
- `OBJECTIVE_RULE` 질문은 외부 판정 입력 전 결과 확정 금지

### 3.4 타이밍 필드 정의 (최신)
- `bettingStartAt = votingEndAt + 5분` (BREAK 고정)
- `revealWindowEndAt = bettingEndAt` (VOTE_RESULT 질문에서 reveal 종료 시점)
- 즉, reveal 기간은 베팅 기간과 동일

## 4. 읽기 API 공개 정책

### 4.1 `GET /api/questions/{id}`
- 공통: `settlementMode`, `status`, `voteSummary.totalCount` 노출
- `OBJECTIVE_RULE`: `yesCount/noCount` 노출 가능
- `VOTE_RESULT`:
- reveal 전(`now < bettingEndAt` 또는 `votingPhase != VOTING_REVEAL_CLOSED`): `yesCount/noCount` 숨김 (`null` 또는 미포함)
- reveal 후: `yesCount/noCount` 공개

### 4.2 `GET /api/votes/status/{questionId}`
- `voteVisibility` 필드 추가:
- `OPEN`
- `HIDDEN_UNTIL_REVEAL`
- `REVEALED`

## 5. 정산 엔진 분기

### 5.1 OBJECTIVE_RULE
- 입력: sourceUrl + finalResult
- 확정: 외부 기준 기반 `YES|NO`

### 5.2 VOTE_RESULT
- 입력: commit/reveal 검증 완료 집계
- 확정:
- `yes > no` -> `YES`
- `no > yes` -> `NO`
- 동점 또는 최소 표 미달 -> `PENDING_REVIEW`

## 6. 데이터 모델

### 6.1 `questions`
- `settlement_mode` 필수
- `voting_phase` (commit/reveal 단계)
- `vote_visibility` (선택: 조회 최적화용)

### 6.2 `vote_commits`
- unique: `(question_id, member_id)`
- 컬럼: `commit_hash`, `committed_at`

### 6.3 `vote_reveals`
- unique: `(question_id, member_id)`
- 컬럼: `choice`, `salt`, `revealed_at`

### 6.4 `vote_summary`
- 최종 집계 캐시
- `VOTE_RESULT`은 reveal 검증 성공 건만 반영

## 7. 보안 및 무결성

- hash 포맷: `sha256(questionId:memberId:choice:salt)`
- reveal 시 hash 재계산 검증 필수
- reveal 마감 후 제출 차단
- commit/reveal idempotency key 권장
- 관리자 예외 처리 시 감사로그 필수

## 8. 프론트 렌더링 규칙

- 결과 소스 라벨 노출:
- `OBJECTIVE_RULE` -> `External Rule`
- `VOTE_RESULT` -> `Community Vote`
- `VOTE_RESULT` + reveal 전:
- yes/no 퍼센트 UI 숨김
- total 참여수/마감시간만 노출

## 9. 마이그레이션 단계

### Phase A (안정화)
- 현행 `POST /api/votes` 유지
- `settlementMode` 저장 및 읽기 응답 분기만 적용

### Phase B (복구)
- `VOTE_RESULT` 질문에 한해 commit/reveal 재활성화
- 질문 단위로 write 경로 분기 적용

### Phase C (운영화)
- 정산/리워드/리포트 자동화 완성
- QA 통과 후 기본 운영 모드 적용

## 10. QA 수용 기준

- `OBJECTIVE_RULE` 질문:
- `POST /api/votes` 성공
- commit/reveal 호출 시 거절
- `VOTE_RESULT` 질문:
- commit/reveal 성공
- direct vote 호출 시 거절
- reveal 전 yes/no 비공개
- reveal 후 yes/no 공개 + 정산 결과 일치
