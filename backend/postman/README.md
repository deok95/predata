# Predata Postman 사용 가이드

## 1) 파일 임포트
- Collection: `Predata_Backend_API_Current.postman_collection.json`
- Environment: `Predata_Local_Current.postman_environment.json`

## 2) 환경 변수
- `baseUrl`: 기본 `http://localhost:8080`
- `token`: JWT 토큰 (로그인 후 수동 입력)
- `memberId`, `questionId`, `betId`, `orderId`, `batchId`, `draftId`, `txHash`

## 3) 권장 순서
1. `POST /api/auth/login`
2. 응답의 JWT를 `token` 변수에 저장
3. 관리자 API 테스트 시 admin 계정 사용

## 4) 기본 관리자 계정 (개발용)
- email: `admin@predata.io`
- password: `123400`

## 5) 참고
- 이 컬렉션은 컨트롤러 기준 자동 생성본입니다.
- POST/PUT/PATCH 본문은 샘플이며, 실제 검증 규칙에 맞게 값 수정이 필요할 수 있습니다.
