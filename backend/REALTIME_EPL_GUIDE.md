# 🔴 실시간 EPL 베팅 시스템 - API 키 발급 가이드

## 🎯 시스템 개요
프리미어 리그 경기를 실시간으로 가져와 **경기 중 베팅**을 가능하게 하는 시스템입니다.

## 📋 주요 기능
- ⚽ **EPL 경기만** 선택적으로 가져오기
- 🔴 **5분마다** 실시간 스코어 업데이트
- 💰 **경기 종료 시까지** 베팅 가능 (기존: 경기 30분 전 마감)
- 📊 **10분마다** 자동 정산
- 🎮 **LIVE 경기 대시보드** 실시간 표시

---

## 🔑 1단계: API 키 발급

### API-FOOTBALL (RapidAPI) - 무료 플랜

#### 1️⃣ 회원가입
```
https://rapidapi.com/api-sports/api/api-football
```
- 우측 상단 "Sign Up" 클릭
- Google 계정으로 빠른 가입 가능

#### 2️⃣ 무료 플랜 구독
- "Subscribe to Test" 버튼 클릭
- **Basic Plan (FREE)** 선택
  - ✅ 100 requests/day
  - ✅ 실시간 데이터 지원
  - ✅ LIVE 스코어 지원

#### 3️⃣ API 키 복사
- 구독 후 "Code Snippets" 섹션에서
- **X-RapidAPI-Key** 복사
- 예: `1234567890abcdefghijk`

---

## ⚙️ 2단계: API 키 설정

### 방법 1: 환경변수 (권장)
```bash
export SPORTS_API_KEY="your-api-key-here"
```

### 방법 2: 설정 파일 직접 수정
```yaml
# backend/src/main/resources/application-local.yml

sports:
  api:
    key: your-api-key-here  # 여기에 API 키 붙여넣기
    enabled: true           # false → true로 변경
```

---

## 🚀 3단계: 서버 재시작

```bash
cd /Users/mac/Desktop/predata/predata/backend
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
pkill -f "spring-boot:run"
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🧪 4단계: 테스트

### 1️⃣ EPL 경기 가져오기
```bash
curl -X POST "http://localhost:8080/api/admin/sports/generate"
```

**예상 응답:**
```json
{
  "created": 10,
  "skipped": 0
}
```

### 2️⃣ 질문 확인
```bash
curl "http://localhost:8080/api/questions"
```

**예상 결과:** EPL 경기 질문들이 생성됨
```json
{
  "id": 6,
  "title": "Will Manchester United beat Liverpool?",
  "category": "SPORTS",
  "status": "OPEN"
}
```

### 3️⃣ LIVE 경기 확인 (경기 시간 중)
```bash
curl "http://localhost:8080/api/admin/sports/live"
```

**예상 응답 (LIVE 경기가 있을 때):**
```json
[
  {
    "matchId": 1,
    "questionId": 6,
    "leagueName": "EPL",
    "homeTeam": "Manchester United",
    "awayTeam": "Liverpool",
    "homeScore": 2,
    "awayScore": 1,
    "status": "LIVE"
  }
]
```

---

## 📊 실시간 업데이트 스케줄

| 작업 | 주기 | 설명 |
|------|------|------|
| 질문 생성 | 매일 00:00 | 향후 14일 EPL 경기 질문 자동 생성 |
| **실시간 스코어 업데이트** | **5분마다** | LIVE 경기 스코어 실시간 반영 |
| **자동 정산** | **10분마다** | 경기 종료 후 빠른 정산 |

---

## 🎮 프론트엔드 - LIVE 경기 대시보드

### 접속 방법
```
http://localhost:3000/admin/questions
```

### 기능
- 🔴 **LIVE 경기 실시간 표시**
- ⚡ **10초마다 자동 새로고침**
- 🎯 **"실시간 베팅하기" 버튼**으로 즉시 베팅 페이지 이동

---

## 📈 API 사용량 관리

### 무료 플랜: 100 requests/day

#### 예상 사용량:
- **질문 생성 (1회/일)**: 1 request
- **실시간 업데이트 (5분마다)**: 
  - 288회/일 (24시간 × 12)
  - LIVE 경기가 있을 때만 API 호출
  - 평균 경기 시간: 2시간
  - 실제: 약 24 requests/경기

#### 최적화 전략:
✅ EPL만 가져오기 (다른 리그 제외)  
✅ LIVE 상태 경기만 실시간 업데이트  
✅ 경기 없을 때 API 호출 안 함  
✅ API 호출 간 0.5초 대기  

---

## 🔴 실시간 베팅 로직

### 기존 vs 새로운 시스템

| 구분 | 기존 | 실시간 베팅 |
|------|------|------------|
| 베팅 마감 | 경기 시작 30분 전 | **경기 종료 시** |
| 스코어 업데이트 | 경기 종료 후 | **5분마다 LIVE** |
| 정산 주기 | 30분마다 | **10분마다** |

### 예시:
```
경기: Man Utd vs Liverpool
시작: 15:00
종료: 17:00

[기존] 베팅 마감: 14:30
[실시간] 베팅 마감: 17:00 ✅

→ 경기 중에도 실시간 스코어를 보며 베팅 가능!
```

---

## 🎯 테스트 시나리오

### 시나리오 1: 질문 자동 생성
```bash
# 1. API 키 설정 확인
curl -X POST "http://localhost:8080/api/admin/sports/generate"

# 2. 생성된 질문 확인
curl "http://localhost:8080/api/questions"

# 3. 마켓플레이스에서 확인
http://localhost:3000/marketplace
```

### 시나리오 2: LIVE 경기 베팅 (경기 시간 중)
```bash
# 1. LIVE 경기 확인
curl "http://localhost:8080/api/admin/sports/live"

# 2. 관리자 페이지에서 LIVE 대시보드 확인
http://localhost:3000/admin/questions

# 3. "실시간 베팅하기" 버튼 클릭 → 베팅
```

### 시나리오 3: 자동 정산
```bash
# 경기 종료 후 10분 이내 자동 정산
# 로그 확인:
tail -f backend/spring-boot.log | grep "AutoGen"
```

---

## ⚠️ 주의사항

### 1. API 요금
- **무료 플랜**: 100 requests/day
- **초과 시**: API 호출 실패 (429 Too Many Requests)
- **해결**: 다음 날까지 대기 또는 유료 플랜

### 2. EPL 경기 일정
- EPL은 주로 **주말**에 경기 집중
- 평일에는 경기가 적을 수 있음
- **Champions League 등 컵 대회**는 제외됨

### 3. 실시간 업데이트
- **5분 주기**이므로 완벽한 실시간은 아님
- 더 빠른 업데이트를 원하면 주기 단축 가능
  - 단, API 요청 수 증가

---

## 🆘 문제 해결

### API 호출 실패 (401 Unauthorized)
```
→ API 키가 잘못되었습니다.
→ application-local.yml에서 키 확인
```

### 경기가 생성되지 않음 (created: 0)
```
→ enabled: true인지 확인
→ 해당 기간에 EPL 경기가 있는지 확인
→ API-FOOTBALL 웹사이트에서 경기 일정 확인
```

### LIVE 경기가 표시되지 않음
```
→ 현재 경기 중인 EPL 경기가 있는지 확인
→ 스케줄러가 작동 중인지 로그 확인
→ 5분마다 업데이트되므로 잠시 대기
```

---

## 🎉 완료!

이제 실시간 EPL 베팅 시스템이 완성되었습니다!

### 다음 스텝:
1. ✅ API 키 발급
2. ✅ `enabled: true` 설정
3. ✅ 서버 재시작
4. ✅ `/admin/questions`에서 LIVE 경기 확인
5. ✅ 실시간 베팅 테스트

---

**문의사항이 있으면 로그를 확인하세요:**
```bash
tail -f backend/spring-boot.log | grep "SportsAPI\|AutoGen\|Scheduler"
```
