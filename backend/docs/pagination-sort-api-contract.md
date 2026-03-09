# PRE(D)ATA V2 Pagination/Sort API Contract

최종 업데이트: 2026-02-23 (UTC)  
범위: 조회(Read) API의 `page/size/sortBy/sortDir` 파라미터 계약

## 공통 규칙

- `page`: 0 이상 정수, 기본값 `0`
- `size`: 1~200, 기본값 `50`
- `sortDir`: `asc|desc` (대소문자 무시), 기본값은 엔드포인트별 상이
- 잘못된 `sortBy`/`sortDir` 값은 서버에서 안전 기본값으로 fallback
- 리스트 응답은 기존 응답 포맷 호환을 위해 구조를 유지하고, 내부적으로만 정렬/페이징 적용

---

## 1) User Read APIs

### `GET /api/questions`

- 목적: 질문 목록 조회
- 파라미터:
  - `page`, `size`
  - `sortBy`: `createdAt | viewCount | totalBetPool`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

예시:
```bash
curl "http://localhost:8080/api/questions?page=0&size=20&sortBy=viewCount&sortDir=desc"
```

### `GET /api/questions/status/{status}`

- 목적: 상태별 질문 목록 조회
- 파라미터:
  - `page`, `size`
  - `sortBy`: `createdAt | viewCount | totalBetPool`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

예시:
```bash
curl "http://localhost:8080/api/questions/status/BETTING?page=0&size=20&sortBy=totalBetPool&sortDir=desc"
```

### `GET /api/activities/me`

- 목적: 내 활동 조회
- 파라미터:
  - `type` (선택): `VOTE | BET | ...`
  - `page`, `size`
  - `sortBy`: `createdAt | amount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

예시:
```bash
curl "http://localhost:8080/api/activities/me?type=BET&page=0&size=30&sortBy=amount&sortDir=desc"
```

### `GET /api/activities/question/{questionId}`

- 목적: 질문 기준 활동 조회
- 파라미터:
  - `type` (선택)
  - `page`, `size`
  - `sortBy`: `createdAt | amount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

### `GET /api/activities/me/question/{questionId}`

- 목적: 내 활동(질문 기준) 조회
- 파라미터:
  - `type` (선택)
  - `page`, `size`
  - `sortBy`: `createdAt | amount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

### `GET /api/portfolio/positions`

- 목적: 내 오픈 포지션 조회
- 파라미터:
  - `page`, `size`
  - `sortBy`: `placedAt | betAmount | estimatedProfitLoss`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=placedAt`
  - `sortDir=desc`

### `GET /api/portfolio/category-breakdown`

- 목적: 카테고리별 성과 조회
- 파라미터:
  - `page`, `size`
  - `sortBy`: `totalBets | profit | winRate`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=totalBets`
  - `sortDir=desc`

### `GET /api/portfolio/accuracy-trend`

- 목적: 정확도 추이 조회
- 파라미터:
  - `page`, `size`
  - `sortBy`: `date | accuracy`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=date`
  - `sortDir=asc`

### `GET /api/questions/me/created`

- 목적: 내가 생성한 질문 목록 조회
- 파라미터:
  - `page`, `size`
- 기본값:
  - `page=0`
  - `size=30`

### `GET /api/members/me/dashboard`

- 목적: 마이페이지 상단 요약 통계 조회
- 파라미터: 없음

---

## 2) Admin Read APIs

### `GET /api/admin/finance/wallet-ledgers`

- 파라미터:
  - `memberId` (필수)
  - `page`, `size`
  - `sortBy`: `createdAt | amount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

### `GET /api/admin/finance/treasury-ledgers`

- 파라미터:
  - `txType` (선택)
  - `page`, `size`
  - `sortBy`: `createdAt | amount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=desc`

### `GET /api/admin/vote-ops/usage`

- 파라미터:
  - `date` (선택, `YYYY-MM-DD`)
  - `page`, `size`
  - `sortBy`: `usedCount | memberId`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=usedCount`
  - `sortDir=desc`

### `GET /api/admin/vote-ops/relay`

- 파라미터:
  - `status` (선택)
  - `page`, `size`
  - `sortBy`: `createdAt | retryCount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=createdAt`
  - `sortDir=asc`

### `GET /api/admin/markets/batches`

- 파라미터:
  - `from`, `to` (선택, ISO_DATE_TIME)
  - `page`, `size`
  - `sortBy`: `startedAt | openedCount | failedCount`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=startedAt`
  - `sortDir=desc`

### `GET /api/admin/markets/batches/{batchId}/candidates`

- 파라미터:
  - `page`, `size`
  - `sortBy`: `rankInCategory | voteCount | createdAt`
  - `sortDir`: `asc | desc`
- 기본값:
  - `sortBy=rankInCategory`
  - `sortDir=asc`

---

## 테스트 커버리지

- `FinanceAdminApiTest`
- `VoteOpsAdminApiTest`
- `MarketBatchAdminApiTest`

위 테스트들에 정렬/페이징 파라미터 케이스가 포함되어 있으며, 회귀 검증 시 같이 실행한다.
