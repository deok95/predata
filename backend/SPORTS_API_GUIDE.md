# 🏆 스포츠 경기 자동 질문 생성 시스템

## 📋 개요
중요 스포츠 리그의 경기를 자동으로 가져와 예측 질문을 생성하고, 경기 종료 후 자동으로 정산하는 시스템입니다.

## 🎯 지원 리그
- ⚽ **축구**: EPL, 라리가, 분데스리가, 세리에A, K리그1
- 🏀 **농구**: NBA (향후 추가 예정)
- ⚾ **야구**: KBO, MLB (향후 추가 예정)

## 🔑 API 키 발급

### API-FOOTBALL (추천)
1. https://www.api-football.com/ 접속
2. 회원가입 (무료 플랜: 100 requests/day)
3. API 키 복사

### 설정 방법
```bash
# 환경변수로 설정
export SPORTS_API_KEY="your-api-key-here"

# 또는 application-local.yml에 직접 입력
sports:
  api:
    key: your-api-key-here
    enabled: true  # API 활성화
```

## 🚀 사용 방법

### 1️⃣ 수동 실행 (테스트용)

#### 스포츠 질문 생성
```bash
curl -X POST "http://localhost:8080/api/admin/sports/generate"
```

**응답 예시:**
```json
{
  "created": 15,
  "skipped": 3
}
```

#### 경기 결과 업데이트
```bash
curl -X POST "http://localhost:8080/api/admin/sports/update-results"
```

**응답 예시:**
```json
{
  "updated": 5
}
```

#### 자동 정산
```bash
curl -X POST "http://localhost:8080/api/admin/sports/settle"
```

**응답 예시:**
```json
{
  "settled": 3
}
```

### 2️⃣ 자동 실행 (스케줄러)

스케줄러는 자동으로 다음 작업을 수행합니다:

| 작업 | 실행 주기 | 설명 |
|------|----------|------|
| 질문 생성 | 매일 자정 (0:00) | 향후 7일 경기 질문 자동 생성 |
| 결과 업데이트 | 매 정각 (1시간마다) | 완료된 경기 결과 가져오기 |
| 자동 정산 | 매 30분 (0분, 30분) | 완료된 경기 질문 자동 정산 |

## 📊 데이터베이스 구조

### sports_matches 테이블
```sql
CREATE TABLE sports_matches (
  match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT,                    -- 연결된 질문 ID
  external_api_id VARCHAR(100) UNIQUE,   -- API 경기 ID
  sport_type VARCHAR(50),                -- FOOTBALL, BASKETBALL, BASEBALL
  league_name VARCHAR(100),              -- EPL, La Liga 등
  home_team VARCHAR(100),                -- 홈팀
  away_team VARCHAR(100),                -- 원정팀
  match_date DATETIME,                   -- 경기 시작 시간
  result VARCHAR(20),                    -- HOME_WIN, AWAY_WIN, DRAW
  home_score INT,                        -- 홈팀 득점
  away_score INT,                        -- 원정팀 득점
  status VARCHAR(20),                    -- SCHEDULED, LIVE, FINISHED
  created_at DATETIME
);
```

## 🎮 생성되는 질문 형식

### 예시 1: EPL
- **제목**: "Will Manchester United beat Liverpool?"
- **카테고리**: SPORTS
- **마감 시간**: 경기 시작 30분 전
- **정산 로직**: 
  - YES = 홈팀 승리 (Man Utd 승)
  - NO = 원정팀 승리 또는 무승부

### 예시 2: K리그1
- **제목**: "Will FC Seoul beat Jeonbuk Hyundai Motors?"
- **카테고리**: SPORTS
- **마감 시간**: 경기 시작 30분 전

## ⚙️ 설정 옵션

### application-local.yml
```yaml
sports:
  api:
    key: ${SPORTS_API_KEY:}  # API 키 (환경변수 우선)
    enabled: false           # true로 변경 시 자동 실행
```

### 트래픽 관리
- API 호출 간 1초 대기 (Rate Limit 방지)
- 중요 리그만 선택적으로 가져오기
- 무료 플랜: 하루 100 requests (약 20개 리그 * 5일 = 가능)

## 🔍 로그 확인

```bash
# 실시간 로그 확인
tail -f backend/spring-boot.log | grep "AutoGen\|Scheduler"

# 특정 작업 로그 검색
grep "질문 생성" backend/spring-boot.log
```

**로그 예시:**
```
[AutoGen] 스포츠 경기 자동 질문 생성 시작
[AutoGen] 가져온 경기 수: 18
[AutoGen] ✅ 생성됨: Manchester United vs Liverpool (Question ID: 10)
[AutoGen] 완료 - 생성: 15, 스킵: 3
[Scheduler] 자동 질문 생성 완료: 생성 15건, 스킵 3건
```

## 🧪 테스트 시나리오

### 1단계: API 키 없이 테스트
```bash
# API 비활성화 상태에서 호출
curl -X POST "http://localhost:8080/api/admin/sports/generate"
# 응답: {"created": 0, "skipped": 0}
```

### 2단계: API 키로 실제 경기 가져오기
```bash
# application-local.yml에서 enabled: true 설정 후
curl -X POST "http://localhost:8080/api/admin/sports/generate"
# 응답: {"created": 15, "skipped": 0}
```

### 3단계: 질문 확인
```bash
curl "http://localhost:8080/api/questions"
# 새로 생성된 스포츠 질문 확인
```

## 🎯 향후 확장 가능 옵션

### 1. 더 많은 스포츠 추가
```kotlin
// SportsApiService.kt
private val nbaLeagues = mapOf("NBA" to 1)
private val kboLeagues = mapOf("KBO" to 1)
```

### 2. 더 정교한 질문 생성
- "Will Team A win by more than 2 goals?"
- "Will the total score be over 2.5?"
- "Will Team A score first?"

### 3. 실시간 라이브 스코어
- 경기 중 배당률 자동 조정
- 라이브 베팅 지원

## 📱 관리자 페이지 연동

`/admin/questions` 페이지에서:
- 자동 생성된 스포츠 질문 확인
- 수동으로 질문 생성/수정/삭제
- 정산 상태 모니터링

## ⚠️ 주의사항

1. **API 요금**: 무료 플랜은 하루 100 requests 제한
2. **Rate Limit**: API 호출 간 1초 대기 필수
3. **시간대**: 모든 시간은 서버 로컬 시간 기준
4. **무승부 처리**: 현재 무승부는 NO로 처리 (향후 DRAW 옵션 추가 가능)

## 🆘 문제 해결

### API 호출 실패
```
[SportsAPI] API 호출 실패: 429 Too Many Requests
→ Rate Limit 초과. 다음 날까지 대기 또는 유료 플랜 고려
```

### 경기가 생성되지 않음
```
[AutoGen] 가져온 경기 수: 0
→ enabled: true 설정 확인
→ API 키 확인
→ 해당 날짜에 경기가 없을 수 있음
```

### 정산이 되지 않음
```
→ 경기 상태가 FINISHED인지 확인
→ match_date가 이미 지났는지 확인
→ question_id가 정확히 연결되어 있는지 확인
```

## 📞 지원

문제가 발생하면:
1. `backend/spring-boot.log` 확인
2. `sports_matches` 테이블 데이터 확인
3. API 키 및 enabled 설정 확인
