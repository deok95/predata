# PRE(D)ATA Backend API

Spring Boot + Kotlin으로 구현된 PRE(D)ATA 예측 시장 백엔드 API 서버입니다.

## 기술 스택

- **Language**: Kotlin 1.9.22
- **Framework**: Spring Boot 3.2.1
- **Database**: MariaDB 10.x
- **ORM**: Spring Data JPA
- **Build Tool**: Gradle (Kotlin DSL)

## 주요 기능

### 1. 투표 시스템 (5-Lock)
- 일일 5개 투표 티켓 제한
- 중복 투표 방지
- 무지성 투표 방지 (latency 체크)
- 자동 티켓 리셋 (날짜 기준)

### 2. 베팅 시스템
- 포인트 기반 베팅
- 비관적 락(Pessimistic Lock)으로 동시성 제어
- 중복 베팅 방지
- 실시간 판돈 업데이트

### 3. 글로벌 페르소나 시스템
- 국가별, 직업별, 연령대별 회원 분류
- 티어 시스템 (BRONZE, SILVER, GOLD 등)
- 티어별 가중치 적용

## 데이터베이스 스키마

```sql
-- 회원
CREATE TABLE members (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    country_code CHAR(2) NOT NULL,
    job_category VARCHAR(50),
    age_group TINYINT,
    tier VARCHAR(20) DEFAULT 'BRONZE',
    tier_weight DECIMAL(3,2) DEFAULT 1.00,
    point_balance BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 예측 질문
CREATE TABLE questions (
    question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title TEXT NOT NULL,
    category VARCHAR(50),
    status VARCHAR(20) DEFAULT 'OPEN',
    total_bet_pool BIGINT DEFAULT 0,
    yes_bet_pool BIGINT DEFAULT 0,
    no_bet_pool BIGINT DEFAULT 0,
    final_result ENUM('YES', 'NO', 'PENDING') DEFAULT 'PENDING',
    expired_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 활동 기록 (투표/베팅)
CREATE TABLE activities (
    activity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    activity_type ENUM('VOTE', 'BET') NOT NULL,
    choice ENUM('YES', 'NO') NOT NULL,
    amount BIGINT DEFAULT 0,
    latency_ms INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member_question_type (member_id, question_id, activity_type)
);

-- 일일 티켓 (5-Lock)
CREATE TABLE daily_tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    remaining_count INT DEFAULT 5,
    reset_date DATE NOT NULL,
    UNIQUE KEY uk_member_date (member_id, reset_date)
);
```

## 설정 방법

### 1. MariaDB 설정

로컬에 MariaDB를 설치하고 데이터베이스를 생성하세요:

```sql
CREATE DATABASE predata CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. application-local.yml 생성

`src/main/resources/application-local.yml` 파일을 생성:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/predata
    username: root
    password: your-actual-password
```

또는 환경 변수로 설정:

```bash
export DB_PASSWORD=your-actual-password
```

### 3. 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
java -jar build/libs/predata-backend-0.0.1-SNAPSHOT.jar
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## API 엔드포인트

### 투표/베팅

- `POST /api/vote` - 투표 실행
- `POST /api/bet` - 베팅 실행

### 질문 조회

- `GET /api/questions` - 전체 질문 목록
- `GET /api/questions/{id}` - 특정 질문 조회
- `GET /api/questions/status/{status}` - 상태별 질문 조회 (OPEN, CLOSED, SETTLED)

### 티켓 관리

- `GET /api/tickets/{memberId}` - 남은 티켓 조회

### 헬스 체크

- `GET /api/health` - 서버 상태 확인

## API 사용 예시

### 투표 실행

```bash
curl -X POST http://localhost:8080/api/vote \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": 1,
    "questionId": 1,
    "choice": "YES",
    "latencyMs": 1500
  }'
```

### 베팅 실행

```bash
curl -X POST http://localhost:8080/api/bet \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": 1,
    "questionId": 1,
    "choice": "YES",
    "amount": 1000,
    "latencyMs": 2000
  }'
```

### 질문 목록 조회

```bash
curl http://localhost:8080/api/questions
```

## 주요 비즈니스 로직

### 5-Lock (일일 투표 제한)

```kotlin
// 티켓 차감
fun consumeTicket(memberId: Long): Boolean {
    val ticket = getOrCreateTodayTicket(memberId)
    if (ticket.remainingCount <= 0) return false
    ticket.remainingCount -= 1
    return true
}
```

### 비관적 락 (베팅 동시성 제어)

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT q FROM Question q WHERE q.id = :id")
fun findByIdWithLock(id: Long): Question?
```

### 중복 참여 방지

```kotlin
// DB 레벨: UNIQUE KEY (member_id, question_id, activity_type)
// 코드 레벨: existsByMemberIdAndQuestionIdAndActivityType()
```

## 프로젝트 구조

```
backend/
├── src/main/kotlin/com/predata/backend/
│   ├── PredataBackendApplication.kt
│   ├── controller/
│   │   └── ActivityController.kt
│   ├── service/
│   │   ├── VoteService.kt
│   │   ├── BetService.kt
│   │   ├── QuestionService.kt
│   │   └── TicketService.kt
│   ├── repository/
│   │   ├── MemberRepository.kt
│   │   ├── QuestionRepository.kt
│   │   ├── ActivityRepository.kt
│   │   └── DailyTicketRepository.kt
│   ├── domain/
│   │   ├── Member.kt
│   │   ├── Question.kt
│   │   ├── Activity.kt
│   │   └── DailyTicket.kt
│   └── dto/
│       └── Dtos.kt
└── src/main/resources/
    └── application.yml
```

## 라이선스

MIT License
