# Front-Backend API Integration Checklist

이 문서는 프론트/백엔드 연동 오류를 줄이기 위한 최소 실행 체크리스트다.
기준일: 2026-02-24

## 1) Contract Baseline 고정

- 기준 문서: `backend/docs/openapi/*` + `backend/docs/API_INTEGRATION_DEFINITION.md`
- 프론트는 `src/services/api.js`를 단일 API 진입점으로 사용
- 컴포넌트에서 요청 body/query를 직접 조합하지 않음

## 2) 배포 전 필수 회귀 체크 (현재 핵심 3건)

### A. Draft Submit `settlementMode`

- 대상 엔드포인트: `POST /api/questions/drafts/{draftId}/submit`
- 허용 값:
- `OBJECTIVE_RULE`
- `VOTE_RESULT`
- 확인 포인트:
- `QuestionSubmitView`에서 위 2개 값만 전송하는지
- `api.js`에서 `mapSettlementMode(...)`를 항상 타는지

### B. Swap Simulate `amount`

- 대상 엔드포인트: `GET /api/swap/simulate`
- 필수 query:
- `questionId`
- `action` (`BUY|SELL`)
- `outcome` (`YES|NO`)
- `amount` (단일 숫자)
- 금지 query:
- `usdcIn`
- `sharesIn`

### C. Signup `passwordConfirm`

- 대상 엔드포인트: `POST /api/auth/signup/complete`
- 필수 body:
- `email`
- `code`
- `password`
- `passwordConfirm`
- 확인 포인트:
- 회원가입 UI에서 `Confirm password` 입력 필드 노출
- `password !== passwordConfirm` 클라이언트 검증 동작

## 3) 실행 절차 (실무용)

1. 백엔드 실행 후 Swagger/OpenAPI에서 대상 엔드포인트 시그니처 확인
2. 프론트에서 실제 요청 1회씩 실행
3. 브라우저 Network 탭에서 Request URL/body를 계약과 1:1 비교
4. 실패 시 `src/services/api.js`만 수정하고 컴포넌트는 수정 최소화
5. 수정 후 같은 시나리오 재실행

## 4) PR Gate (머지 조건)

- 아래 3개가 모두 통과하지 않으면 머지 금지
- Draft Submit `settlementMode` 계약 일치
- Swap Simulate `amount` 계약 일치
- Signup `passwordConfirm` 계약/검증 일치

## 5) 권장 운영 규칙

- 백엔드 API 변경 시:
- OpenAPI 갱신
- `backend/docs/API_INTEGRATION_DEFINITION.md` 갱신
- 프론트 `src/services/api.js` 갱신
- 한 PR에서 동시에 반영

