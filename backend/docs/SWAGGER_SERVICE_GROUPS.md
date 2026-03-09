# Swagger Service Groups (V2)

프론트 연동 기준은 `OpenApiConfig`의 그룹 문서를 단일 소스로 사용한다.

## 1) 문서 URL
- 통합: `/v3/api-docs`
- 그룹별: `/v3/api-docs/{group}`
- UI: `/swagger-ui/index.html`

## 2) 서비스 단위 그룹
- `auth`
  - `/api/auth/**`
- `member-social`
  - `/api/members/**`, `/api/users/**`, `/api/notifications/**`, `/api/referrals/**`, `/api/leaderboard/**`, `/api/tiers/**`, `/api/badges/**`
- `question`
  - `/api/questions/**`, `/api/admin/questions/**`, `/api/admin/settings/question-generator`
- `voting`
  - `/api/votes/**`, `/api/activities/**`, `/api/admin/voting/**`, `/api/admin/vote-ops/**`, `/api/tickets/**`, `/api/voting-pass/**`
- `market-amm`
  - `/api/swap/**`, `/api/pool/**`, `/api/admin/markets/**`, `/api/questions/top3`
- `settlement-reward`
  - `/api/settlements/**`, `/api/admin/settlements/**`, `/api/rewards/**`, `/api/admin/rewards/**`, `/api/analysis/**`, `/api/analytics/**`, `/api/premium-data/**`
- `finance-wallet`
  - `/api/payments/**`, `/api/portfolio/**`, `/api/transactions/**`, `/api/blockchain/**`, `/api/admin/finance/**`
- `ops-admin`
  - `/api/admin/**`, `/api/health`, `/api/sports/**`, `/api/betting/**`

## 3) 프론트 전달 방식 (권장)
1. 프론트가 사용하는 그룹만 내려받는다.
2. 엔드포인트 매칭은 path/HTTP method 기준으로만 수행한다.
3. 레거시 경로(`410`)는 클라이언트 라우팅에서 제거한다.

## 4) 스냅샷 파일 (로컬 생성본)
- 생성 위치: `backend/docs/openapi`
- 통합: `all.json`
- 그룹별:
  - `auth.json`
  - `member-social.json`
  - `question.json`
  - `voting.json`
  - `market-amm.json`
  - `settlement-reward.json`
  - `finance-wallet.json`
  - `ops-admin.json`
