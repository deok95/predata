# 프론트엔드 페이지별 API 매핑

## App.tsx (Home)
- `GET /api/questions` → 마켓 카드 목록 (replaces MARKETS mock)
- CATEGORIES는 그대로 유지 (프론트 상수)

## VotePageView.tsx
- `GET /api/votes/feed` → 투표 피드 (replaces ALL_VOTE_QUESTIONS)
- `POST /api/votes` → 투표 실행
- `GET /api/votes/status/{questionId}` → 투표 가능 여부 확인

## VoteDetailView.tsx
- `GET /api/questions/{id}` → 질문 상세
- `GET /api/votes/status/{questionId}` → 투표 가능 여부
- `POST /api/votes` → 투표 실행
- `GET /api/questions/{id}/comments` → 댓글
- `POST /api/questions/{id}/comments` → 댓글 작성

## BetDetailView.tsx
- `GET /api/questions/{id}` → 질문 상세
- `GET /api/pool/{questionId}` → AMM 풀 상태 (YES/NO 가격)
- `GET /api/swap/simulate` → 매수/매도 시뮬레이션
- `POST /api/swap` → 스왑 실행
- `GET /api/swap/history/{questionId}` → 거래 히스토리
- `GET /api/swap/my-shares/{questionId}` → 내 보유 포지션
- `GET /api/questions/{id}/comments` → 댓글
- `POST /api/questions/{id}/comments` → 댓글 작성

## MyPageView.tsx
- `GET /api/members/me` → 내 프로필 (replaces MOCK_USER)
- `GET /api/members/me/dashboard` → 대시보드 통계
- `GET /api/portfolio/positions` → 오픈 포지션 (replaces MOCK_POSITIONS)
- `GET /api/settlements/history/me` → 정산 이력 (replaces MOCK_HISTORY)
- `GET /api/questions/me/created` → 내가 만든 질문 (replaces MOCK_CREATED_QUESTIONS)
- `GET /api/activities/me` → 활동 피드 (replaces MOCK_ACTIVITY_FEED)
- `PUT /api/users/me/profile` → 프로필 수정

## QuestionSubmitView.tsx
- `POST /api/questions/drafts/open` → 초안 생성
- `POST /api/questions/drafts/{draftId}/submit` → 제출
- `POST /api/questions/drafts/{draftId}/cancel` → 취소

## ExploreView.tsx
- `GET /api/questions` → 카테고리별 질문 목록 (replaces EXPLORE_CATEGORIES mock)
- `GET /api/questions?category=X&status=BETTING` → 베팅 중인 마켓

## AuthModals.tsx
- `POST /api/auth/login` → 로그인
- `POST /api/auth/send-code` → 인증 코드 발송
- `POST /api/auth/verify-code` → 코드 확인
- `POST /api/auth/complete-signup` → 가입 완료
- `POST /api/auth/google` → 구글 로그인
- `POST /api/auth/wallet/nonce` → MetaMask 로그인 nonce 발급
- `POST /api/auth/wallet` → MetaMask 서명 검증 로그인

## MyPageView.tsx (Wallet Link)
- `POST /api/members/wallet/nonce` → 지갑 연결 nonce 발급
- `PUT /api/members/wallet` → 지갑 서명 검증 후 계정 연결/해제
