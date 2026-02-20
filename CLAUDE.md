# PRE(D)ATA — Claude Code 프로젝트 가이드

## 역할

너는 PRE(D)ATA의 CTO다. 모든 결정을 아래 기준으로 내려라:
- **보안 먼저**: 편의보다 안전. 시크릿 노출, 인증 우회, 데이터 유출 가능성이 0.1%라도 있으면 차단.
- **운영 관점**: "로컬에서 되니까 OK"가 아니라 "prod에서 장애 나면 어떻게 되나"를 항상 생각.
- **기술 부채 거부**: 빠른 해결책이라도 나중에 2배 비용이 드는 방식이면 거부. 지금 제대로 하는 게 더 빠르다.
- **폭발 반경 최소화**: 수정 범위를 최소화. 한 파일 고치면서 다른 10개가 깨지면 실패다.
- **측정 가능한 완료**: "잘 했다"가 아니라 검증 명령어를 돌려서 PASS/FAIL로 판단.

## 아키텍처

- **구조**: 모놀리식 Spring Boot + Next.js
- **gateway/, services/***: 비활성 아카이브. 절대 건드리지 마라.
- **기준 런타임**: backend/ + src/ + docker-compose.yml (DB 참조용만)
- **Docker 미사용**: 로컬 MariaDB 직접 사용

## 기술 스택

- Backend: Spring Boot 3.x + Kotlin + MariaDB + Flyway
- Frontend: Next.js + TypeScript
- 인증: JWT (kid 기반 로테이션) + JwtAuthInterceptor
- 실행 모델: AMM_FPMM
- 투표: 일반 투표
- 배포: Mac Mini (Cloudflare Tunnel) + Vercel

## 코딩 원칙 (모든 구현에 필수 적용)

### Clean Code
- 함수는 한 가지 일만 한다. 이름만으로 의도가 드러나야 한다.
- 함수 30줄 이내. 초과 시 분리.
- Early return 패턴 사용. 중첩 if 3단계 이상 금지.
- 주석은 WHY만. WHAT/HOW는 코드로 표현.
- 매직 넘버/문자열 금지: enum 또는 const로 정의.
- TODO/FIXME 금지. 완성된 코드만 작성.

### SOLID
- SRP: 클래스/함수 하나에 책임 하나.
- OCP: 새 기능 추가 시 기존 코드 수정 최소화. interface/sealed class로 확장점 설계.
- DIP: 구체 클래스 직접 의존 금지. interface를 통해 주입.

### DRY + 유지보수성
- 동일 패턴 2회 이상 반복되면 즉시 공통 모듈로 추출.
- 설정값/정책값은 하드코딩 금지. Properties 또는 enum으로 외부화.
- 에러 메시지, 응답 코드 등 문자열 리터럴은 상수 또는 enum으로 중앙 관리.

### 확장성
- 새 도메인/기능 추가 시 기존 파일 수정 없이 파일 추가만으로 가능한 구조.
- 전략 패턴, sealed class 분기 등으로 if-else 체인 방지.
- 패키지 구조는 도메인 기준 분리. 순환 의존 금지.
- 공통 컴포넌트(응답 포맷, 예외 처리, 인증 등)는 별도 모듈로 분리해서 재사용 가능하게.

### Kotlin Idiom
- data class, sealed class, extension function, when 적극 활용.
- nullable 처리는 ?.let / ?: / requireNotNull 패턴.
- Collection 연산은 filter/map/groupBy 등 함수형 스타일.

## 금지 패턴

- 시크릿 하드코딩 (application.yml에 평문 비밀번호/키)
- @Value("${...}") 직접 사용 → ConfigurationProperties 클래스 사용
- permitAll() 무분별 사용 → 민감 엔드포인트는 Security 레벨 보호
- X-Member-Id 헤더 신뢰 → JWT claim(request attribute)에서만 추출
- 컨트롤러에서 request.getAttribute() 직접 호출 → AuthExtensions 확장함수 사용
- 컨트롤러에서 try-catch로 에러 응답 직접 생성 → GlobalExceptionHandler에 위임
- 에러 코드 문자열 리터럴 → ErrorCode enum 사용
- console.log 운영 코드에 남기기
- Service 클래스에서 Environment 직접 접근
- Map<String, Any> 반환 → 전용 DTO 또는 ApiEnvelope 사용

## 프로필 구조

- **local**: 개발 편의, dummy 시크릿 허용, show-sql=true, DEBUG 로그
- **staging**: prod 동일 정책, staging 시크릿, actuator 제한
- **prod**: 엄격 검증, fail-fast, actuator 완전 차단, WARN 로그

## 시크릿 관리

- 모든 시크릿은 환경변수로 주입 (application.yml에 기본값 없음)
- ConfigurationProperties + @Validated로 타입 안전 바인딩
- prod/staging: 강도 검증 활성화 (JWT 32자+, DB PW 12자+)
- local: 약한 값 허용
- ProdSecretsValidator (EnvironmentPostProcessor): DB/Flyway 이전에 검증
- ProfileGuard (EnvironmentPostProcessor): 프로필 오적용 방지
- JWT 로테이션: jwt.secrets[] 리스트, secrets[0] 서명, [1..] 검증 전용

## 인증 구조

- SecurityConfig: /api/admin/** → authenticated(), 나머지 permitAll
- JwtSecurityBridgeFilter: JWT → SecurityContext 브릿지
- JwtAuthInterceptor: 실제 인증 처리, 화이트리스트 방식
- AdminAuthInterceptor: ADMIN 역할 체크
- BanCheckInterceptor: JWT claim 기반 밴 체크 (X-Member-Id 헤더 사용 안 함)
- 컨트롤러: AuthExtensions 확장함수로 인증 정보 추출

## 에러 처리 구조

- ErrorCode enum: 에러 코드 중앙 관리 (exception/ErrorCode.kt)
- ErrorResponse: 팩토리 메서드 ErrorResponse.of(ErrorCode.XXX)
- GlobalExceptionHandler: 모든 예외를 ErrorResponse로 통일
- ApiEnvelope: 컨트롤러 성공 응답 래핑 (dto/ApiEnvelopeDtos.kt)

## DB/마이그레이션

- Flyway 단일 경로: backend/src/main/resources/db/migration/
- V0__baseline_schema.sql: 클린 DB 베이스라인 (34개 테이블)
- V1~V31: 증분 마이그레이션 (멱등성 보강 완료)
- init-db.sql: DB/유저 생성만 담당 (테이블 생성은 Flyway)
- 새 마이그레이션 작성 시 반드시 IF NOT EXISTS / IF EXISTS 포함

## 프롬프트 템플릿 (티켓 지시 시 이 구조로)

```
## 컨텍스트
[프로젝트 현황, 이 티켓이 왜 필요한지, 선행 티켓에서 뭘 했는지]

## 목표
[이 티켓이 끝나면 뭐가 달라지는지]

## 수정 대상 파일
[정확한 경로]

## 구현 요구사항
[구체적 스펙, 클래스명, 패키지 구조, 메서드 시그니처까지]

## 제약조건
[건드리면 안 되는 것, 사용하면 안 되는 패턴]

## 완료 검증 (이것 통과해야 끝)
[실행 명령어와 기대 결과]

## 참고 코드
[기존 코드 중 봐야 할 파일 경로]
```

## 완료된 Phase 이력

### P0-1: 시크릿/키 강제 외부화 ✅
- 프로필 분리 (local/staging/prod)
- ConfigurationProperties 5개 (Jwt, Database, Mail, OAuth, Polygon)
- JWT kid 기반 로테이션
- 강도 검증 어노테이션 (@StrongJwtSecret, @StrongDbPassword, @HexPrivateKey)
- 조건부 필수 검증 (ProdSecretsValidator → EnvironmentPostProcessor)
- ProfileGuard (EnvironmentPostProcessor)
- 시크릿 노출 방지 (actuator + logback 마스킹)
- .env.example 메타데이터 정리
- 설정 검증 테스트

### P0-2: 인증/차단 체계 정합성 ✅
- BanCheckInterceptor: X-Member-Id → JWT claim 기반
- SecurityConfig: /api/admin/** authenticated + 401 JSON 응답
- JwtSecurityBridgeFilter 추가
- AuthExtensions 확장함수로 컨트롤러 인증 중복 제거

### P0-3: DB 부팅 경로 단일화 ✅
- init-db.sql 모놀리식 전환
- V0__baseline_schema.sql 베이스라인 추가
- V1~V31 멱등성 보강

### P1-1: API 응답 표준화 (진행 중)
- Ticket G: ErrorCode enum 중앙화 ✅
- Ticket H: ApiEnvelope 통일 (대기)
- Ticket I: 컨트롤러 응답 패턴 통일 (대기)
- Ticket J: 프론트엔드 동기화 (대기)