# 🔴 실시간 EPL 베팅 시스템 - 완성!

## ✅ 구현 완료 항목

### 1️⃣ **EPL 전용 시스템**
- ✅ 프리미어 리그 경기만 선택적으로 가져오기
- ✅ 다른 리그 제외 (트래픽 절약)
- ✅ 14일 선행 경기 조회

### 2️⃣ **실시간 업데이트**
- ✅ **5분마다** 실시간 스코어 업데이트
- ✅ **10분마다** 자동 정산
- ✅ LIVE 상태 경기 자동 추적

### 3️⃣ **경기 중 베팅**
- ✅ 마감 시간: 경기 종료 시 (기존: 경기 30분 전)
- ✅ 실시간 스코어를 보며 베팅 가능
- ✅ LIVE 경기 중에도 계속 베팅 가능

### 4️⃣ **LIVE 경기 대시보드**
- ✅ 실시간 LIVE 경기 표시
- ✅ 10초마다 자동 새로고침
- ✅ 홈/원정팀 스코어 실시간 표시
- ✅ "실시간 베팅하기" 버튼

### 5️⃣ **API & 데이터베이스**
- ✅ `sports_matches` 테이블
- ✅ `SportsApiService` (API-FOOTBALL 연동)
- ✅ `QuestionAutoGenerationService`
- ✅ `SportsSchedulerService`
- ✅ `SportsManagementController`

---

## 🔑 API 키 발급 방법

### 1단계: RapidAPI 가입
```
https://rapidapi.com/api-sports/api/api-football
```

### 2단계: Basic Plan (무료) 구독
- 100 requests/day
- 실시간 데이터 지원

### 3단계: API 키 복사
- X-RapidAPI-Key 복사

### 4단계: 설정
```yaml
# backend/src/main/resources/application-local.yml

sports:
  api:
    key: your-api-key-here  # API 키 입력
    enabled: true           # 활성화
```

### 5단계: 서버 재시작
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🚀 테스트 방법

### 1. EPL 경기 자동 생성
```bash
curl -X POST "http://localhost:8080/api/admin/sports/generate"
```

### 2. LIVE 경기 확인
```bash
curl "http://localhost:8080/api/admin/sports/live"
```

### 3. 프론트엔드에서 확인
```
http://localhost:3000/admin/questions
→ LIVE 경기 대시보드에서 실시간 스코어 확인
→ "실시간 베팅하기" 버튼 클릭
```

---

## 📊 실시간 업데이트 스케줄

| 작업 | 주기 | 설명 |
|------|------|------|
| 질문 생성 | 매일 00:00 | 향후 14일 EPL 경기 |
| **스코어 업데이트** | **5분마다** | LIVE 경기 실시간 반영 |
| **자동 정산** | **10분마다** | 경기 종료 후 빠른 정산 |

---

## 🎮 프론트엔드 기능

### `/admin/questions` 페이지
- 🔴 LIVE 경기 실시간 대시보드
- ⚡ 10초마다 자동 새로고침
- 🏆 리그 배지 (EPL)
- 📊 실시간 스코어보드
- 🎯 "실시간 베팅하기" 버튼

### `/marketplace` 페이지
- EPL 질문 필터링 (SPORTS 카테고리)
- 실시간 배당률 표시

### `/question/[id]` 페이지
- 경기 종료 시까지 베팅 가능
- 실시간 배당률 업데이트 (5초마다)

---

## 📈 API 사용량 최적화

### 무료 플랜: 100 requests/day

#### 최적화 전략:
✅ **EPL만 가져오기** (1 league)  
✅ **LIVE 경기만 업데이트** (경기 시간 중)  
✅ **API 호출 간격: 0.5초**  
✅ **경기 없을 때 호출 안 함**

#### 예상 사용량:
- 질문 생성: 1 request/day
- LIVE 업데이트: ~24 requests/경기 (2시간 × 5분 주기)
- EPL 평균: 주말 10경기 → ~240 requests (초과 가능)

#### 추천:
- 주말에 API 호출 집중
- 평일에는 자동 생성 스킵
- 또는 유료 플랜 고려 (Mega Plan: $9.99/월, 30,000 requests)

---

## 🎯 실시간 베팅 시나리오

### 예시: Man Utd vs Liverpool
```
경기 시작: 15:00
경기 종료: 17:00

[타임라인]
14:00 → 질문 생성 (자동)
15:00 → 경기 시작 🔴 LIVE
15:05 → 스코어 업데이트: 0-0
15:10 → 스코어 업데이트: 1-0 (Man Utd 선제골!)
15:15 → 베팅 가능! (실시간)
...
17:00 → 경기 종료
17:10 → 자동 정산 ✅
```

---

## 🔴 LIVE 베팅의 장점

### 1. 더 정확한 예측
- 실시간 경기 상황을 보며 베팅
- 선제골, 부상, 퇴장 등 반영

### 2. 더 많은 베팅 기회
- 경기 전 + 경기 중 모두 베팅 가능
- 베팅 풀 증가 → 수수료 증가

### 3. 더 활발한 데이터 수집
- 티케터들의 실시간 투표
- 경기 상황에 따른 의견 변화 추적

---

## ⚠️ 주의사항

### 1. API 키 필수
- **현재 상태**: `enabled: false` (API 비활성화)
- **활성화**: API 키 발급 후 `enabled: true`

### 2. EPL 경기 일정
- 주로 **주말**에 경기 집중
- 평일에는 경기가 적을 수 있음

### 3. API 요금 초과
- 무료: 100 requests/day
- 초과 시: 429 Too Many Requests
- 해결: 다음 날까지 대기 또는 유료 플랜

---

## 📚 문서

- **`REALTIME_EPL_GUIDE.md`**: 상세 가이드
- **`SPORTS_API_GUIDE.md`**: 기본 가이드

---

## 🎉 완료!

실시간 EPL 베팅 시스템이 완성되었습니다!

### 다음 스텝:
1. ✅ API 키 발급 (https://rapidapi.com)
2. ✅ `enabled: true` 설정
3. ✅ 서버 재시작
4. ✅ 테스트!

---

**API 키를 발급받으시면 바로 실시간 EPL 베팅을 시작하실 수 있습니다!** ⚽🔥
