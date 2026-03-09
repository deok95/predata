# PRE(D)ATA V2 v1.8 대비 현재 구현 플로우 변경/추가점

기준 시점: 현재 `backend` 코드베이스 (`/api` 컨트롤러 + 서비스 + 설정)

## 1. 핵심 변경 요약

1. 인증 플로우가 문서의 "MetaMask nonce 서명 로그인 전용"과 다르게, 이메일/비밀번호, 지갑 주소 로그인, Google OAuth를 함께 지원함.
2. 출금 API가 문서안(`POST /api/me/withdrawals`)과 다르게 `POST /api/payments/withdraw`로 구현됨.
3. 출금 승인 플로우(`approve/reject`)가 전용 관리자 출금 API로 분리되어 있지 않음(현재는 즉시 처리 중심).
4. 투표 쓰기 표준 경로가 `POST /api/votes`로 정착됨. 레거시 `/api/vote`, `/api/votes/commit`, `/api/votes/reveal`은 410(Gone) 처리됨.
5. 질문 생성은 Draft 기반(`open/submit/cancel`) + idempotency 키 검증이 구현됨.
6. Top3 마켓 오픈 배치, 온체인 릴레이 큐, 지갑/원장(ledger) 계층이 운영 코드에 추가됨.

## 2. 서비스 단위 구현 상태 (v1.8 기준 대비)

### 2.1 Auth
1. 구현 API: `/api/auth/*`
2. 현재 방식: 이메일/비밀번호 + 지갑주소 + Google OAuth
3. v1.8과 차이: MetaMask nonce challenge API 계약이 문서 수준으로 명시된 형태와 직접 일치하지 않음

### 2.2 Question / Draft
1. 구현 API: `/api/questions/drafts/open|{id}/submit|{id}/cancel`, `/api/questions/credits/status`
2. 구현 내용: draft lock, 만료, 소유권, idempotency, 크레딧/활성질문 제약
3. v1.8과 일치: Draft 2-step 및 제약 검증 중심 설계

### 2.3 Voting
1. 구현 API: `/api/votes`, `/api/votes/status/{questionId}`, `/api/votes/feed`
2. 구현 내용: 질문당 1회, 일일 제한, 조회 분리, 피드 모드(foryou/following/top10)
3. 변경점: 레거시 vote 경로 제거(410)

### 2.4 Market Batch / Top3 / AMM
1. 구현 API: `/api/admin/markets/batches/*`, `/api/questions/top3`, `/api/swap`, `/api/pool/{questionId}`
2. 구현 내용: 카테고리 Top3 선별/오픈 배치, 시드/수수료 검증, FPMM 스왑
3. v1.8과 일치: Top3 고정 및 AMM 중심 거래

### 2.5 Settlement
1. 구현 API: `/api/admin/settlements/questions/{id}/settle-auto|settle|finalize|cancel`, `/api/settlements/history/me`
2. 구현 내용: 자동/수동 정산 경로, finalize 단계, 히스토리 조회
3. 변경점: `cancel`은 현재 410으로 비활성 정책 적용

### 2.6 Wallet / Treasury / Payments
1. 구현 API: `/api/payments/verify-deposit`, `/api/payments/withdraw`, `/api/admin/finance/*`
2. 구현 내용: 지갑 원장 credit/debit/lock, treasury ledger 반영, deposit indexer(확정 수 기반) 설정 존재
3. v1.8과 차이: API 경로 체계가 `/api/me/withdrawals` 기반 문서와 불일치

### 2.7 Social / Profile
1. 구현 API: `/api/users/*`, `/api/questions/{id}/comments`, `/api/notifications/*`
2. 구현 내용: 프로필 수정, 팔로우/언팔로우, 댓글
3. 문서 외 확장: v1.8 본문 범위를 넘어선 소셜 기능이 포함됨

## 3. 현재 Swagger(OpenAPI) 서비스 그룹

Swagger UI: `/swagger-ui.html`  
전체 스펙: `/v3/api-docs`  
서비스 그룹 스펙:

1. `/v3/api-docs/auth`
2. `/v3/api-docs/member-social`
3. `/v3/api-docs/question`
4. `/v3/api-docs/voting`
5. `/v3/api-docs/market-amm`
6. `/v3/api-docs/settlement-reward`
7. `/v3/api-docs/finance-wallet`
8. `/v3/api-docs/ops-admin`

## 4. 프론트 전달 시 권고

1. 프론트는 Postman/README 대신 OpenAPI 그룹 문서를 계약 기준으로 사용.
2. 레거시 경로(`/api/vote`, `/api/votes/commit`, `/api/votes/reveal`) 호출 금지.
3. 결제/출금 경로는 현재 구현(`/api/payments/*`) 기준으로 반영.
