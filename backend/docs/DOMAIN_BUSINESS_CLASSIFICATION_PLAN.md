# Domain/Business 분류 기준 및 1차 매핑 계획

## 1. 목표
1. 지금은 리팩터링 실행보다 "분류 기준"을 먼저 고정한다.
2. 이후 모든 분리 작업은 본 문서 기준으로만 진행한다.

## 2. 분류 기준 (고정)
1. Domain
   - 질문: "무슨 규칙이 참/거짓인가?"
   - 예: 상태 전이, 한도/정렬/수식/배분 규칙
   - 금지: Repository, Redis, 외부 API, HTTP DTO 의존
2. Business(Application)
   - 질문: "유스케이스를 어떤 순서로 수행하는가?"
   - 예: 트랜잭션 경계, 락 획득, 정책 호출, 저장, 이벤트 발행
3. Infrastructure
   - 질문: "어디에 저장/연결하는가?"
   - 예: JPA Repository, Redis, Blockchain, 외부 API 연동
4. Interface
   - 질문: "입출력 변환을 어떻게 하는가?"
   - 예: Controller, Request/Response DTO 매핑, 인증 컨텍스트 바인딩

## 3. 현재 코드의 분류 상태
1. Domain (이미 있음)
   - `domain/**` 엔티티/enum
   - `domain/policy/QuestionPhaseTransitionPolicy`
   - `domain/policy/VotePolicy`
2. Business (현재 혼합도 높음)
   - `service/**` 다수 파일이 정책 + 오케스트레이션 + I/O를 함께 포함
3. Infrastructure
   - `repository/**`, `config/**`, `service/relay/**`, `service/wallet/DepositIndexerScheduler`
4. Interface
   - `controller/**`, `dto/**`

## 4. 모듈별 분류 대상 (우선순위)
1. P0: Settlement / Fee / Wallet
   - 이유: 수식/분배/원장 정합성 핵심
   - 대상 서비스: `SettlementService`, `FeePoolService`, `WithdrawalService`, `PaymentVerificationService`, `WalletBalanceService`
   - 추출할 Domain policy:
     - `SettlementPolicy` (승자/패자, payout, 상태전이 판단)
     - `FeeDistributionPolicy` (platform/creator/voter 분배)
     - `WithdrawalPolicy` (한도/수수료/승인 필요 여부)
2. P1: Market Batch / Top3 / AMM
   - 대상 서비스: `CategoryTop3SelectionService`, `MarketBatchService`, `MarketOpenService`, `SwapService`, `FpmmMathEngine`
   - 추출할 Domain policy:
     - `Top3SelectionPolicy` (정렬/동점 규칙)
     - `CanonicalDuplicatePolicy`
     - `AmmTradePolicy` (슬리피지/입력 유효성)
3. P2: Question Draft / Quota
   - 대상 서비스: `QuestionDraftService`, `QuestionQuotaService`, `QuestionContentValidationService`
   - 추출할 Domain policy:
     - `QuestionQuotaPolicy`
     - `QuestionContentPolicy`
     - `DraftLifecyclePolicy`
4. P3: Social / Analytics / Ops
   - 대상 서비스: `SocialService`, `AnalyticsService`, `AuditService` 등
   - 목표: 정책/조회 조합 단순화

## 5. 작업 단위 템플릿 (반복 규칙)
1. Step A: 서비스에서 "순수 규칙 코드"만 식별
2. Step B: `domain/policy/*`로 추출 (순수 함수)
3. Step C: 기존 service는 정책 호출 + 트랜잭션/저장만 남김
4. Step D: 테스트 분리
   - Domain policy unit test
   - Service integration test

## 6. 완료 판정 기준
1. Domain policy는 infrastructure import가 0개여야 함
2. Service 파일에서 계산/분배 상수 하드코딩이 제거되어야 함
3. 동일 규칙 중복 구현이 1곳으로 수렴되어야 함
4. 기존 API 동작/에러코드는 변경되지 않아야 함

## 7. 다음 실행 범위 제안
1. P2(`Question Draft / Quota`) 잔여 오케스트레이션 슬림화
2. 한 번에 1개 유스케이스씩 분리
3. 각 유스케이스마다 "정책 추출 + 테스트 + 컴파일"을 한 세트로 마감

## 8. 진행 현황 (업데이트)
1. 완료
   - `QuestionPhaseTransitionPolicy` 추출
   - `VotePolicy` 추출
   - `FeeDistributionPolicy` 추출
   - `WithdrawalPolicy` 추출
   - `DepositPolicy` 추출
   - `WalletPolicy` 추출
   - `OnChainTransferPolicy` 추출
   - `WalletLedgerPolicy` 추출
   - `SettlementPolicy` 1차 확장(시작/확정 가능 조건, payout, collateral 무결성)
   - `SettlementService` 오케스트레이션 분리 (`SettlementPayoutService`, `SettlementHistoryQueryService`, `SettlementPostProcessService` 위임)
   - `PaymentVerificationService` 온체인 이벤트 검증 규칙 정책화 + 도메인 정책 테스트 확장
   - `Top3SelectionPolicy` 추출 (`CategoryTop3SelectionService` 정렬/선별 규칙 위임)
   - `CanonicalDuplicatePolicy` 추출 (canonical hash 결정 단일화)
   - `AmmTradePolicy` 추출 (`SwapService` 입력/슬리피지/seed 유효성 위임)
   - `MarketOpenPolicy` 추출 (`MarketOpenService` 상태판정/복구조건/오류메시지 처리 위임)
   - `BatchLifecyclePolicy` 추출 (`MarketBatchService` 멱등 skip/최종 상태 계산 위임)
   - `QuestionQuotaPolicy` 추출 (`QuestionQuotaService` 생성 제한 규칙 위임)
   - `QuestionContentPolicy` 추출 (`QuestionContentValidationService`/`QuestionDraftService` 콘텐츠 검증 위임)
   - `DraftLifecyclePolicy` 추출 (`QuestionDraftService` draft 접근/상태 판단 위임)
   - `DraftQuestionPolicy` 추출 (`QuestionDraftService` 크레딧/기간/수수료 계산 위임)
   - `SocialPolicy` 추출 (`SocialService` username/follow/page/comment 규칙 위임)
   - `AnalyticsPolicy` 추출 (`AnalyticsService` 퍼센트/괴리율/품질점수/의심지표 규칙 위임)
   - `AuditQueryPolicy` 추출 (`AuditService` 조회 조건 라우팅 규칙 위임)
   - `AbusingDetectionPolicy` 추출 (`AbusingDetectionService` 괴리 임계치/위험도/권고 규칙 위임)
   - `SybilDetectionPolicy` 추출 (`SybilDetectionService` IP/패턴 의심 판정 및 메시지 규칙 위임)
   - `BotTradingPolicy` 추출 (`BotTradingService` 봇 수/선택/금액/커밋해시 규칙 위임)
   - `SybilGuardPolicy` 추출 (`SybilGuardService` 계정연령/이력 기반 보상 자격 판정 위임)
   - `BotSchedulerPolicy` 추출 (`BotScheduler` 실행 게이트 판정 위임)
   - `BotMemberPolicy` 추출 (`BotMemberService` 봇 계정 수량/이메일/프로필 생성 규칙 위임)
   - 컨트롤러 전반 `@Tag` 적용 완료 (서비스 그룹 태깅 통일)
   - Swagger 서비스 그룹 문서화 + 그룹별 스냅샷 생성: `docs/SWAGGER_SERVICE_GROUPS.md`, `docs/openapi/*.json`
   - OpenAPI-계약 미스매치 CI 자동검증 추가 (`scripts/qa/check-openapi-contract-match.mjs`, `.github/workflows/ci.yml`)
   - 정책 단위 테스트 추가: `AbusingDetectionPolicyTest`, `SybilDetectionPolicyTest`, `BotTradingPolicyTest`, `SybilGuardPolicyTest`, `BotSchedulerPolicyTest`, `BotMemberPolicyTest`
2. 진행 중
   - `P3` 잔여 Ops 서비스 정리 및 정책 커버리지 보강
   - Ops 서비스 통합 테스트 보강 (`SybilGuardServiceTest`, `BotMemberServiceTest`)
   - Ops 서비스 오케스트레이션 테스트 보강 (`BotSchedulerServiceTest`, `BotTradingServiceTest`)
3. 다음 대상
   - P3 마감: Ops 관련 테스트 스위트 묶음 실행 + 회귀 점검
