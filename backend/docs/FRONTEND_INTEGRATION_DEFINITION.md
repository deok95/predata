# PRE(D)ATA 프론트엔드 연동 정의서 (Frontend Integration Definition)

- 기준 코드:
  - `src/services/api.ts`
  - `src/hooks/useApi.ts`
  - `src/services/websocket.ts`
  - `src/views/*`
- 백엔드 계약 SSOT:
  - `backend/docs/openapi/all.json`
  - `backend/docs/API_INTEGRATION_DEFINITION.md`

## 1. 목적

이 문서는 프론트엔드가 백엔드 API를 호출할 때 따라야 하는 실제 구현 기준(요청/응답/상태관리/에러분기)을 정의한다.
백엔드 OpenAPI는 "계약", 본 문서는 "프론트 구현 규칙"이다.

## 2. 런타임/환경 변수

- API Base: `NEXT_PUBLIC_API_URL` (기본값: `http://localhost:8080`)
- WebSocket URL: `NEXT_PUBLIC_API_URL`을 `ws://` 또는 `wss://`로 변환 후 `/ws` 접속
- Google Sign-In: `NEXT_PUBLIC_GOOGLE_CLIENT_ID`

## 3. 프론트 API 레이어 구조

1. `apiFetch` (`src/services/api.ts`)
- 모든 HTTP 호출의 공통 래퍼
- `ApiEnvelope` 파싱
- non-JSON/empty-body/401 처리
- JWT 헤더 자동 주입

2. 도메인 API 모듈
- `authApi`, `questionApi`, `voteApi`, `marketApi`, `portfolioApi`, `activityApi`, `memberApi`, `paymentApi`, `leaderboardApi`

3. Hook 레이어 (`src/hooks/useApi.ts`)
- 페이지에서 직접 fetch를 호출하지 않고 훅을 통해 상태(`data/loading/error`)를 일관 처리

4. View 레이어 (`src/views/*`)
- 훅을 통해 데이터 바인딩
- 요청 payload 조합은 `api.ts`로 집중

## 4. 인증/토큰 규칙

- 토큰 키: `localStorage.predata_token`
- 멤버 키: `localStorage.predata_member_id`
- 인증 헤더: `Authorization: Bearer <token>`
- 401 처리 정책:
  - 세션 검증 성격 endpoint(`members/me`, `auth/refresh`, `auth/login`)에서만 인증 상태 클리어
  - 그 외 endpoint 401은 즉시 로그아웃시키지 않음

## 5. 공통 응답 처리 규칙

- 성공: `json.success !== false` 이고 `res.ok`인 경우 `json.data` 반환
- 실패: `json.error.message` 우선, 없으면 `json.message`, 없으면 `HTTP <status>`
- 204 또는 빈 본문은 `null` 반환
- 목록 응답은 `unwrapPaginated`로 통일 처리
  - 허용 형태: `[]`, `{items:[]}`, `{content:[]}`

## 6. 도메인별 API 정의 (프론트 사용 기준)

### 6.1 Auth

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/send-code`
- `POST /api/auth/verify-code`
- `POST /api/auth/complete-signup`
- `POST /api/auth/google`
- `POST /api/auth/google/complete-registration`
- `POST /api/auth/wallet/nonce`
- `POST /api/auth/wallet`

프론트 강제 규칙:
- `complete-signup`은 `passwordConfirm` 필드 필수 전송
- 구글 로그인은 `googleToken` 필드명 고정

### 6.2 Question/Draft/Comment

- `GET /api/questions`
- `GET /api/questions/{id}`
- `GET /api/questions/me/created`
- `GET /api/questions/status/{status}`
- `GET /api/questions/credits/status`
- `POST /api/questions/drafts/open`
- `POST /api/questions/drafts/{draftId}/submit`
- `POST /api/questions/drafts/{draftId}/cancel`
- `GET /api/questions/{id}/comments`
- `POST /api/questions/{id}/comments`
- `DELETE /api/questions/{id}/comments/{commentId}`

프론트 강제 규칙:
- `drafts/open` 호출 시 `X-Idempotency-Key` 헤더 전송
- `drafts/submit` 호출 시:
  - `submitIdempotencyKey` body + `X-Idempotency-Key` 헤더 동시 전송
  - `settlementMode`는 내부 `mapSettlementMode(...)`를 통해 정규화
  - `voteWindowType`는 내부 `mapVoteWindowType(...)`를 통해 정규화

### 6.3 Voting

- `POST /api/votes`
- `GET /api/votes/status/{questionId}`
- `GET /api/votes/feed`
- `GET /api/votes/daily-remaining`
- `POST /api/votes/commit`
- `POST /api/votes/reveal`

프론트 강제 규칙:
- 투표 body 필드명은 `choice` 사용 (`side` 금지)
- `feed` 파라미터 정규화:
  - `limit -> size`
  - `timeFilter -> window`
  - window 값은 `H6|D1|D3` 정규화

### 6.4 Market/AMM

- `GET /api/pool/{questionId}`
- `POST /api/swap`
- `GET /api/swap/simulate`
- `GET /api/swap/price-history/{questionId}`
- `GET /api/swap/history/{questionId}`
- `GET /api/swap/my-history/{questionId}`
- `GET /api/swap/my-shares/{questionId}`

프론트 강제 규칙:
- `simulate` 쿼리는 `amount` 단일 필드 사용 (`usdcIn`, `sharesIn` 금지)
- `swap`은 `action/outcome/usdcIn|sharesIn` 형태로 전송

### 6.5 Portfolio/Settlement/Activity

- `GET /api/portfolio/summary`
- `GET /api/portfolio/positions`
- `GET /api/portfolio/category-breakdown`
- `GET /api/portfolio/accuracy-trend`
- `GET /api/settlements/history/me`
- `GET /api/activities/me`
- `GET /api/activities/question/{questionId}`
- `GET /api/activities/me/question/{questionId}`

### 6.6 Member/Social

- `GET /api/members/me`
- `GET /api/members/me/dashboard`
- `GET /api/users/{id}`
- `PUT /api/users/me/profile`
- `POST /api/users/me/avatar` (multipart)
- `POST /api/users/{id}/follow`
- `DELETE /api/users/{id}/follow`
- `GET /api/users/{id}/followers`
- `GET /api/users/{id}/following`
- `POST /api/members/wallet/nonce`
- `PUT /api/members/wallet`

### 6.7 Payment/Wallet/Leaderboard

- `GET /api/payments/config`
- `POST /api/payments/verify-deposit`
- `POST /api/payments/withdraw`
- `GET /api/transactions/my`
- `GET /api/leaderboard/top?limit={n}`
- `GET /api/leaderboard/member/{memberId}`

## 7. 페이지별 연동 매핑

- `App.tsx`, `ExploreView.tsx`
  - 질문 목록/카테고리 필터: `/api/questions`
- `VotePageView.tsx`, `VoteDetailView.tsx`
  - 투표 피드/실행/상태: `/api/votes/*`
- `BetDetailView.tsx`
  - 풀 상태/시뮬레이션/스왑/히스토리: `/api/pool/*`, `/api/swap/*`
- `MyPageView.tsx`
  - 프로필/대시보드/포트폴리오/정산/활동/결제/출금
- `QuestionSubmitView.tsx`
  - 질문 draft open/submit/cancel
- `LeaderboardView.tsx`
  - 리더보드 top/member
- `AuthModals.tsx`
  - 로그인/회원가입/인증코드/구글/지갑 로그인

## 8. 실시간(WebSocket) 연동 정의

- Broker URL: `${NEXT_PUBLIC_API_URL -> ws}/ws`
- 사용 토픽:
  - `/topic/markets`
  - `/topic/votes`
  - `/topic/pool/{questionId}`
- 정책:
  - 연결 전 subscribe 요청은 pending queue에 저장 후 connect 시 flush
  - 메시지 body는 JSON parse 실패 시 무시
  - 기본 재연결 간격: 5초

## 9. 에러 코드 분기 표준

UI 분기는 문자열 message가 아니라 `error.code` 기준으로 처리한다.

필수 처리 코드:
- 인증: `UNAUTHORIZED`, `INVALID_TOKEN`, `FORBIDDEN`, `ACCOUNT_BANNED`
- 투표: `VOTING_CLOSED`, `ALREADY_VOTED`, `DAILY_LIMIT_EXCEEDED`
- Draft: `DAILY_CREATE_LIMIT_EXCEEDED`, `ACTIVE_QUESTION_EXISTS`, `DRAFT_EXPIRED`, `DUPLICATE_QUESTION`, `CREDIT_LOCK_TIMEOUT`
- 공통: `BAD_REQUEST`, `VALIDATION_FAILED`, `CONFLICT`, `NOT_FOUND`, `INTERNAL_ERROR`

## 10. QA 게이트

배포 전 프론트-백엔드 계약 검증은 아래를 모두 통과해야 한다.

1. `npm run qa:api-unit`
2. `npm run qa:api-contract`
3. `npm run lint && npx tsc --noEmit && npm run build`

## 11. 변경 관리 규칙

- API 경로/메서드/필드 변경 시 동시 수정 필수:
  - `backend/docs/openapi/*.json`
  - `backend/docs/API_INTEGRATION_DEFINITION.md`
  - `backend/docs/FRONTEND_INTEGRATION_DEFINITION.md` (본 문서)
  - `src/services/api.ts`
- 컴포넌트에서 API payload를 직접 조합하지 않고 `api.ts`에 규칙을 집중한다.
