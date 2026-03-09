# Domain vs Business(Use-case) 분리 기준 (PRE(D)ATA)

## 1. 분리 원칙
1. Domain: "무엇이 가능한가/불가능한가" 규칙만 가진다.
2. Business(Application): "어떤 순서로 실행할 것인가"를 오케스트레이션한다.
3. Infrastructure: DB/Redis/외부 API/체인 I/O를 담당한다.
4. Controller: HTTP 입출력 변환 + 인증 컨텍스트만 처리한다.

## 2. 코드 배치 기준
1. Domain
   - 경로: `domain/**`, `domain/policy/**`
   - 내용: 상태 전이 규칙, 제한 정책, 정산 계산 규칙
   - 금지: Repository, Redis, HTTP DTO 직접 의존
2. Application(Service)
   - 경로: `service/**`
   - 내용: 트랜잭션 경계, 순서 제어, 실패 처리, audit 호출
   - 역할: Domain policy 호출 + Repository/infra 조합
3. Infrastructure
   - 경로: `repository/**`, `config/**`, `service/*(외부 어댑터)`
   - 내용: 영속성/캐시/외부 연동 구현

## 3. 1차 분리 적용 내역
1. Question phase FSM:
   - `QuestionLifecycleService` 내부 전이 규칙을 `domain/policy/QuestionPhaseTransitionPolicy`로 분리
2. Vote 정책:
   - 일일 한도 및 투표 가능 판단 규칙을 `domain/policy/VotePolicy`로 분리
   - `VoteCommandService`, `VoteQueryService`는 정책 호출 + 저장 오케스트레이션만 담당

## 4. 다음 분리 우선순위
1. Settlement 수식/배분 로직 -> `domain/policy/SettlementPolicy`
2. Market Top3 선별 정렬/중복 canonical 규칙 -> `domain/policy/MarketSelectionPolicy`
3. Withdrawal 한도/승인 규칙 -> `domain/policy/WithdrawalPolicy`
4. Question Draft 제약(연간 크레딧, active question gate) -> `domain/policy/QuestionQuotaPolicy`
