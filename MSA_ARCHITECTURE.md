# Predata MSA Architecture

## 🎯 **MSA 설계 원칙**

1. **단일 책임 원칙** - 각 서비스는 하나의 비즈니스 도메인만 담당
2. **독립 배포** - 서비스별로 독립적으로 배포/확장 가능
3. **데이터 격리** - 각 서비스는 자체 DB를 가짐
4. **느슨한 결합** - 서비스 간 통신은 API 또는 이벤트로만

---

## 📊 **서비스 분리**

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway                              │
│                    (Kong / Spring Cloud Gateway)                 │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Member       │   │  Question     │   │  Betting      │
│  Service      │   │  Service      │   │  Service      │
│  :8081        │   │  :8082        │   │  :8083        │
└───────────────┘   └───────────────┘   └───────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
   [member_db]        [question_db]       [betting_db]

        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Settlement   │   │  Data         │   │  Sports       │
│  Service      │   │  Service      │   │  Service      │
│  :8084        │   │  :8085        │   │  :8086        │
└───────────────┘   └───────────────┘   └───────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
  [settlement_db]      [data_db]          [sports_db]

                     ┌───────────────┐
                     │  Blockchain   │
                     │  Service      │
                     │  :8087        │
                     └───────────────┘
                            │
                            ▼
                   [Redis / Mock Chain]
```

---

## 🔧 **서비스별 책임**

### 1. **Member Service** (`:8081`)
**책임**: 회원 관리, 인증, 티어
**엔티티**: Member, DailyTicket
**API**:
- `POST /api/members` - 회원가입
- `GET /api/members/{id}` - 회원 조회
- `GET /api/members/by-email` - 이메일로 조회
- `PUT /api/members/{id}/tier` - 티어 업데이트
- `GET /api/members/{id}/tickets` - 티켓 현황

---

### 2. **Question Service** (`:8082`)
**책임**: 질문/마켓 CRUD
**엔티티**: Question
**API**:
- `POST /api/questions` - 질문 생성
- `GET /api/questions` - 질문 목록
- `GET /api/questions/{id}` - 질문 상세
- `PUT /api/questions/{id}` - 질문 수정
- `DELETE /api/questions/{id}` - 질문 삭제
- `GET /api/questions/{id}/odds` - 배당률 조회

---

### 3. **Betting Service** (`:8083`)
**책임**: 투표/베팅 처리
**엔티티**: Activity (Vote, Bet)
**API**:
- `POST /api/votes` - 투표
- `POST /api/bets` - 베팅
- `GET /api/activities/member/{memberId}` - 내 활동 내역
- `GET /api/activities/question/{questionId}` - 질문별 활동

**이벤트 발행**:
- `BetPlacedEvent` → Settlement, Blockchain
- `VotePlacedEvent` → Data

---

### 4. **Settlement Service** (`:8084`)
**책임**: 정산, 보상 분배
**엔티티**: Settlement, Reward
**API**:
- `POST /api/settlements/question/{id}` - 정산 실행
- `GET /api/settlements/question/{id}` - 정산 결과
- `GET /api/rewards/member/{memberId}` - 보상 내역

**이벤트 구독**:
- `QuestionSettledEvent` ← Question
- `BetPlacedEvent` ← Betting

**이벤트 발행**:
- `SettlementCompletedEvent` → Member, Blockchain

---

### 5. **Data Service** (`:8085`)
**책임**: 데이터 분석, 품질 검증, 프리미엄 데이터
**엔티티**: (읽기 전용, 다른 서비스 데이터 집계)
**API**:
- `GET /api/analytics/dashboard` - 대시보드
- `GET /api/analytics/demographics` - 인구통계
- `GET /api/analytics/gap-analysis` - 갭 분석
- `GET /api/premium-data` - 프리미엄 데이터 추출
- `GET /api/abusing/check/{memberId}` - 어뷰징 체크

---

### 6. **Sports Service** (`:8086`)
**책임**: 스포츠 API 연동, 자동 질문 생성
**엔티티**: SportsMatch
**API**:
- `GET /api/sports/matches` - 경기 목록
- `GET /api/sports/live` - 실시간 경기
- `POST /api/sports/sync` - 수동 동기화
- `GET /api/sports/suspension/{questionId}` - 베팅 중지 상태

**스케줄러**:
- 경기 데이터 자동 갱신
- 자동 정산 트리거

---

### 7. **Blockchain Service** (`:8087`)
**책임**: 온체인 기록 (Mock/Real)
**저장소**: Redis (Mock), Base L2 (Real)
**API**:
- `GET /api/blockchain/status` - 체인 상태
- `GET /api/blockchain/question/{id}` - 온체인 데이터
- `POST /api/blockchain/batch` - 배치 기록

**이벤트 구독**:
- `BetPlacedEvent` ← Betting (배치 큐)
- `SettlementCompletedEvent` ← Settlement

---

## 📡 **서비스 간 통신**

### REST API (동기)
```
Member ←→ Betting (포인트 차감 확인)
Question ←→ Betting (질문 상태 확인)
Settlement → Member (포인트 지급)
```

### Event Bus (비동기 - Redis Pub/Sub)
```
Betting → [BetPlacedEvent] → Settlement, Blockchain, Data
Settlement → [SettlementCompletedEvent] → Member, Blockchain
Sports → [MatchUpdatedEvent] → Question
Sports → [GoalScoredEvent] → Betting (베팅 중지)
```

---

## 🗄️ **데이터베이스 분리**

```sql
-- Member Service DB
CREATE DATABASE predata_member;
USE predata_member;
CREATE TABLE members (...);
CREATE TABLE daily_tickets (...);

-- Question Service DB
CREATE DATABASE predata_question;
USE predata_question;
CREATE TABLE questions (...);

-- Betting Service DB
CREATE DATABASE predata_betting;
USE predata_betting;
CREATE TABLE activities (...);

-- Settlement Service DB
CREATE DATABASE predata_settlement;
USE predata_settlement;
CREATE TABLE settlements (...);
CREATE TABLE rewards (...);

-- Sports Service DB
CREATE DATABASE predata_sports;
USE predata_sports;
CREATE TABLE sports_matches (...);
```

---

## 🚀 **구현 우선순위**

### Phase 1: Core Services (MVP)
1. ✅ **Member Service** - 회원, 티어
2. ✅ **Question Service** - 질문 관리
3. ✅ **Betting Service** - 투표/베팅

### Phase 2: Business Logic
4. 🔲 **Settlement Service** - 정산
5. 🔲 **Data Service** - 분석

### Phase 3: External Integration
6. 🔲 **Sports Service** - 스포츠 연동
7. 🔲 **Blockchain Service** - 온체인

---

## 📁 **프로젝트 구조**

```
predata/
├── services/
│   ├── member-service/       # :8081
│   │   ├── src/
│   │   ├── build.gradle.kts
│   │   └── Dockerfile
│   ├── question-service/     # :8082
│   ├── betting-service/      # :8083
│   ├── settlement-service/   # :8084
│   ├── data-service/         # :8085
│   ├── sports-service/       # :8086
│   └── blockchain-service/   # :8087
├── common/                   # 공통 모듈
│   ├── dto/
│   └── events/
├── gateway/                  # API Gateway
├── docker-compose.yml
└── frontend/
```

---

## ⚡ **기술 스택**

| 구성요소 | 기술 |
|---------|------|
| Language | Kotlin |
| Framework | Spring Boot 3.2 |
| Database | MariaDB (서비스별 분리) |
| Event Bus | Redis Pub/Sub |
| API Gateway | Spring Cloud Gateway |
| Container | Docker + Docker Compose |
| Service Discovery | Eureka (선택) |

---

## 🎯 **시작하기**

```bash
# 1. 모든 서비스 시작
docker-compose up -d

# 2. 개별 서비스 시작
cd services/member-service && ./gradlew bootRun
cd services/question-service && ./gradlew bootRun
# ...
```

---

## 📝 **다음 단계**

1. **Phase 1 구현**: Member, Question, Betting 서비스 분리
2. 서비스 간 통신 테스트
3. Docker Compose 설정
4. API Gateway 추가
