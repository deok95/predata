# PRE(D)ATA API 연동 정의서 (QA 통과 기준)

## 1. 기준 버전
- 작성 시각(UTC): `2026-02-24T17:47:13Z`
- 백엔드 QA 결과:
  - `cd backend && ./gradlew test --no-daemon` 통과
  - `node scripts/qa/check-openapi-contract-match.mjs` 통과 (`missing=0`)
- 스펙 단일 소스(SSOT):
  - 통합 OpenAPI: `backend/docs/openapi/all.json`
  - 그룹 OpenAPI: `backend/docs/openapi/*.json`

## 2. 서버/인증 규칙
- Base URL: `http://127.0.0.1:8080`
- 인증 헤더: `Authorization: Bearer <JWT>`
- 공개(비인증) 허용 주요 GET:
  - `/api/questions/**` (단, `/api/questions/credits*`, `/api/questions/drafts*` 제외)
  - `/api/members/{id}`
  - `/api/users/{id}`, `/api/users/{id}/followers`, `/api/users/{id}/following`
  - `/api/questions/{id}/comments`
  - `/api/pool/{questionId}`, `/api/swap/simulate`, `/api/swap/price-history/{questionId}`, `/api/swap/history/{questionId}`
  - `/api/votes/status/{questionId}`, `/api/votes/feed` (Authorization 없으면 anonymous 처리)

## 3. 공통 응답 계약
- 성공 응답(`ApiEnvelope<T>`):
```json
{
  "success": true,
  "data": {},
  "error": null,
  "message": null,
  "timestamp": "2026-02-24T17:47:13"
}
```
- 실패 응답(`ErrorResponse` 또는 `ApiEnvelope.error`):
```json
{
  "code": "BAD_REQUEST",
  "message": "Invalid request.",
  "status": 400,
  "details": [],
  "timestamp": "2026-02-24T17:47:13"
}
```

## 4. 서비스 그룹별 문서
- `auth` (`7` paths): `backend/docs/openapi/auth.json`
- `member-social` (`25` paths): `backend/docs/openapi/member-social.json`
- `question` (`23` paths): `backend/docs/openapi/question.json`
- `voting` (`21` paths): `backend/docs/openapi/voting.json`
- `market-amm` (`14` paths): `backend/docs/openapi/market-amm.json`
- `settlement-reward` (`23` paths): `backend/docs/openapi/settlement-reward.json`
- `finance-wallet` (`12` paths): `backend/docs/openapi/finance-wallet.json`
- `ops-admin` (`57` paths): `backend/docs/openapi/ops-admin.json`

## 5. 프론트 연동 우선 엔드포인트 (계약 검증 완료 24개)
- `GET /api/questions`
- `GET /api/questions/{id}`
- `GET /api/votes/status/{questionId}`
- `GET /api/portfolio/summary`
- `GET /api/portfolio/positions`
- `GET /api/settlements/history/me`
- `GET /api/admin/markets/batches`
- `GET /api/admin/markets/batches/{batchId}/candidates`
- `GET /api/admin/markets/batches/{batchId}/summary`
- `GET /api/members/me/dashboard`
- `GET /api/questions/me/created`
- `GET /api/questions/status/{status}`
- `GET /api/activities/me`
- `GET /api/activities/question/{questionId}`
- `GET /api/activities/me/question/{questionId}`
- `GET /api/portfolio/category-breakdown`
- `GET /api/portfolio/accuracy-trend`
- `GET /api/admin/finance/wallet-ledgers`
- `GET /api/admin/finance/treasury-ledgers`
- `GET /api/admin/vote-ops/usage`
- `GET /api/admin/vote-ops/relay`
- `POST /api/votes`
- `POST /api/swap`
- `POST /api/questions/drafts/*` (open/submit/cancel)

## 6. 연동 고정 규칙
- 경로/메서드 변경은 OpenAPI 스냅샷 갱신 + 문서 동시 갱신 없이 금지
- 프론트는 OpenAPI 기준 필드명 그대로 사용 (임의 alias 금지)
- 에러 처리는 `code` 기준 분기 (message 하드코딩 금지)
- 시간 필드는 UTC ISO-8601 문자열로 처리
- 정렬/페이지 파라미터는 각 endpoint의 OpenAPI 파라미터 정의를 우선

## 7. 프론트 전달 패키지
- 필수 전달 파일:
  - `backend/docs/API_INTEGRATION_DEFINITION.md` (본 문서)
  - `backend/docs/openapi/all.json`
  - `backend/docs/openapi/question.json`
  - `backend/docs/openapi/voting.json`
  - `backend/docs/openapi/market-amm.json`
  - `backend/docs/openapi/finance-wallet.json`
  - `backend/docs/openapi/member-social.json`
- 검증 리포트:
  - `backend/docs/FRONT_API_CONTRACT_MISMATCH_REPORT.md` (`missing=0`)
