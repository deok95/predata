# 프론트 API 스타터 가이드

## 1. 기준 문서
- 연동 정의서: `backend/docs/API_INTEGRATION_DEFINITION.md`
- 프론트 연동 정의서: `backend/docs/FRONTEND_INTEGRATION_DEFINITION.md`
- OpenAPI 통합: `backend/docs/openapi/all.json`
- 미스매치 리포트: `backend/docs/FRONT_API_CONTRACT_MISMATCH_REPORT.md`

## 2. 바로 사용 가능한 SDK 파일
- `backend/docs/frontend-sdk/predata-api-types.ts`
- `backend/docs/frontend-sdk/predata-endpoints.ts`
- `backend/docs/frontend-sdk/predata-api-client.ts`

## 3. 권장 초기 연결 순서
1. 인증: `/api/auth/login` 연결
2. 목록/상세: `/api/questions`, `/api/questions/{id}`
3. 투표: `/api/votes/status/{questionId}`, `/api/votes`
4. AMM: `/api/swap`, `/api/pool/{questionId}`
5. 마이페이지: `/api/portfolio/summary`, `/api/portfolio/positions`, `/api/settlements/history/me`

## 4. 구현 규칙
- JWT 필요 API는 `Authorization: Bearer <token>` 고정
- 응답은 `ApiEnvelope` 우선 파싱 (`success=false`면 `error.code`로 분기)
- 오류 UI 분기는 `message`가 아니라 `error.code` 기준으로 처리
- 시간은 UTC 문자열 그대로 클라이언트에서 포맷만 수행

## 5. 필수 에러 코드 핸들링
- 인증: `UNAUTHORIZED`, `INVALID_TOKEN`, `FORBIDDEN`, `ACCOUNT_BANNED`
- 투표: `VOTING_CLOSED`, `ALREADY_VOTED`, `DAILY_LIMIT_EXCEEDED`
- 질문 draft: `DAILY_CREATE_LIMIT_EXCEEDED`, `ACTIVE_QUESTION_EXISTS`, `DRAFT_EXPIRED`, `DUPLICATE_QUESTION`, `CREDIT_LOCK_TIMEOUT`
- 공통: `BAD_REQUEST`, `VALIDATION_FAILED`, `CONFLICT`, `NOT_FOUND`, `INTERNAL_ERROR`

## 6. 최소 사용 예시
```ts
import { PredataApiClient } from './predata-api-client';

const api = new PredataApiClient('http://127.0.0.1:8080', () => localStorage.getItem('accessToken'));

const questions = await api.getQuestions({ page: 0, size: 20, sortBy: 'createdAt', sortDir: 'desc' });
const detail = await api.getQuestionDetail(questions[0].id);
const voteStatus = await api.getVoteStatus(detail.id);

if (voteStatus.canVote) {
  await api.postVote({ questionId: detail.id, choice: 'YES' });
}
```
