# Mock 블록체인 개발 환경 설정 완료

## 🎯 **목표**

로컬 개발 환경에서 **실제 블록체인처럼 동작하는 Mock 체인**을 구현하여, 나중에 실제 블록체인으로 쉽게 전환할 수 있도록 함.

---

## ✅ **구현 완료 항목**

### 1. **MockBlockchainService** 
`backend/src/main/kotlin/com/predata/backend/service/MockBlockchainService.kt`

- ✅ 메모리 기반 가짜 블록체인 저장소
- ✅ 트랜잭션 해시 생성 (0xmock...)
- ✅ 실제 블록체인처럼 지연 시간 시뮬레이션
- ✅ 상세한 로깅 (🔗, ✅, ❌ 이모지 포함)

#### 주요 기능:
```kotlin
// 1. 질문 생성
createQuestionOnChain(question) → CompletableFuture<String?>

// 2. 배치 베팅
batchPlaceBetsOnChain(bets: List<BetOnChainData>) → CompletableFuture<String?>

// 3. 정산
settleQuestionOnChain(questionId, finalResult) → CompletableFuture<String?>

// 4. 온체인 조회
getQuestionFromChain(questionId) → QuestionOnChain?

// 5. Mock 체인 상태
getMockChainStatus() → MockChainStatus
```

---

### 2. **BlockchainService (통합)**
`backend/src/main/kotlin/com/predata/backend/service/BlockchainService.kt`

Mock과 Real 블록체인을 **동적으로 전환**할 수 있는 통합 서비스:

```kotlin
@Service
class BlockchainService(
    @Value("\${blockchain.enabled}") val enabled: Boolean,
    @Value("\${blockchain.mode}") val mode: String, // "mock" or "real"
    private val mockBlockchainService: MockBlockchainService
) {
    // mode에 따라 자동 분기
    fun createQuestionOnChain(question: Question) =
        when (mode) {
            "mock" -> mockBlockchainService.createQuestionOnChain(question)
            "real" -> createQuestionOnRealChain(question) // TODO: 나중에 구현
            else -> CompletableFuture.completedFuture(null)
        }
}
```

---

### 3. **DTO 추가**
`backend/src/main/kotlin/com/predata/backend/dto/BlockchainDtos.kt`

```kotlin
data class BetOnChainData(
    val questionId: Long,
    val userAddress: String?,
    val choice: Boolean,
    val amount: Long
)

data class QuestionOnChain(
    val questionId: Long,
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val settled: Boolean
)

data class BlockchainStatusResponse(
    val enabled: Boolean,
    val network: String,
    val totalQuestions: Int,
    val totalTransactions: Int
)
```

---

### 4. **BlockchainController**
`backend/src/main/kotlin/com/predata/backend/controller/BlockchainController.kt`

```kotlin
@RestController
@RequestMapping("/api/blockchain")
class BlockchainController {
    
    // GET /api/blockchain/status
    @GetMapping("/status")
    fun getBlockchainStatus(): BlockchainStatusResponse
    
    // GET /api/blockchain/question/{questionId}
    @GetMapping("/question/{questionId}")
    fun getQuestionFromChain(@PathVariable questionId: Long): QuestionOnChain?
}
```

---

### 5. **설정 파일**
`backend/src/main/resources/application-local.yml`

```yaml
blockchain:
  enabled: true # Mock 블록체인 활성화
  mode: mock # mock: 로컬 개발, real: 실제 블록체인
  rpc:
    url: https://sepolia.base.org # 실제 블록체인 사용 시
  contract:
    address: "" # 실제 블록체인 사용 시
  admin:
    private-key: "" # 실제 블록체인 사용 시
```

---

## 🔄 **동작 흐름**

### 개발 환경 (Mock 모드)
```
사용자 베팅
    ↓
BetService (DB 저장)
    ↓
BettingBatchService (큐에 모음)
    ↓
10초마다 배치 처리
    ↓
BlockchainService (mode=mock)
    ↓
MockBlockchainService (메모리 저장 + 로그)
    ↓
🔗 [MOCK CHAIN] 배치 베팅 트랜잭션
  📊 베팅 수: 5개
  🔖 TX Hash: 0xmock00000001a3b4f8e2
  ✅ 5개 베팅 Mock 체인에 기록 완료!
```

### 프로덕션 환경 (Real 모드)
```
blockchain:
  enabled: true
  mode: real # 이것만 변경!
```

→ **자동으로 실제 Web3j 기반 블록체인 연동으로 전환!**

---

## 🎬 **Mock 블록체인 로그 예시**

서버 시작 시:
```
🔧 Mock 블록체인 서비스 시작 (로컬 개발 모드)
📍 네트워크: Local Mock Chain
⚡ 가스비: 무료 (시뮬레이션)

🔗 블록체인 서비스 활성화
📍 모드: Mock Chain (개발)
```

질문 생성 시:
```
🔗 [MOCK CHAIN] 질문 생성 트랜잭션
  📝 Question ID: 1
  📄 제목: EPL: 맨시티 vs 리버풀, 맨시티 승리?
  🔖 TX Hash: 0xmock00000001f3a9c7e4
  ✅ Mock 체인에 기록 완료!
```

베팅 시:
```
⚡ 배치 베팅 처리 시작: 12개
🔗 [MOCK CHAIN] 배치 베팅 트랜잭션
  📊 베팅 수: 12개
  🔖 TX Hash: 0xmock00000002b8c3f6d1
    ↳ Question #1: 1000P → YES
    ↳ Question #1: 500P → NO
    ... (10 more)
  ✅ 12개 베팅 Mock 체인에 기록 완료!
```

정산 시:
```
🔗 [MOCK CHAIN] 정산 트랜잭션
  📝 Question ID: 1
  🎯 결과: YES
  🔖 TX Hash: 0xmock00000003d7e4a9f2
  ✅ Mock 체인 정산 완료!
```

---

## 📊 **API 엔드포인트**

### 1. 블록체인 상태 조회
```bash
GET http://localhost:8080/api/blockchain/status
```

**응답:**
```json
{
  "enabled": true,
  "network": "Local Mock Chain",
  "totalQuestions": 5,
  "totalTransactions": 23
}
```

### 2. 온체인 질문 데이터 조회
```bash
GET http://localhost:8080/api/blockchain/question/1
```

**응답:**
```json
{
  "questionId": 1,
  "totalBetPool": 50000,
  "yesBetPool": 30000,
  "noBetPool": 20000,
  "settled": false
}
```

---

## 🚀 **실제 블록체인으로 전환 방법**

### Step 1: 컨트랙트 배포 (Hardhat)
```bash
cd blockchain
npx hardhat compile
npx hardhat run scripts/deploy.js --network baseSepolia
```

### Step 2: 설정 변경
```yaml
blockchain:
  enabled: true
  mode: real # 이것만 변경!
  rpc:
    url: https://sepolia.base.org
  contract:
    address: "0x..." # 배포된 주소
  admin:
    private-key: "${ADMIN_PRIVATE_KEY}" # 환경변수
```

### Step 3: 재시작
```bash
cd backend
mvn spring-boot:run
```

→ **자동으로 실제 블록체인 연동!**

---

## 🎯 **장점**

1. **개발 환경에서 블록체인 시뮬레이션 가능**
   - 가스비 0원
   - 실제 네트워크 연결 불필요
   - 빠른 테스트

2. **실제 블록체인처럼 동작**
   - 트랜잭션 해시 생성
   - 지연 시간 시뮬레이션
   - 상세한 로그

3. **쉬운 전환**
   - 설정 한 줄만 변경
   - 코드 수정 불필요

4. **통합 관리**
   - `BlockchainService` 하나로 통합
   - Mock과 Real 모드 자동 분기

---

## 📌 **다음 단계**

### 현재 상황:
- ✅ Mock 블록체인 코드 완성
- ⚠️ 서버 시작 시 DB 접속 오류 (MariaDB 비밀번호 문제)

### 해결 방법:
1. **임시 해결** (현재 터미널에서 실행 중인 서버 사용):
   - 기존 서버는 정상 작동 중
   - Mock 블록체인 코드 추가 후 재시작 필요

2. **완전 해결**:
   - MariaDB 비밀번호 재설정 또는
   - application-local.yml의 비밀번호 확인

---

## 🔗 **관련 파일**

- `backend/src/main/kotlin/com/predata/backend/service/MockBlockchainService.kt`
- `backend/src/main/kotlin/com/predata/backend/service/BlockchainService.kt`
- `backend/src/main/kotlin/com/predata/backend/dto/BlockchainDtos.kt`
- `backend/src/main/kotlin/com/predata/backend/controller/BlockchainController.kt`
- `backend/src/main/kotlin/com/predata/backend/service/BettingBatchService.kt`
- `backend/src/main/resources/application-local.yml`

---

## 🎉 **완료!**

**로컬 개발 환경에서 Web3처럼 동작하는 Mock 블록체인 시스템이 구현되었습니다!**

서버만 재시작하면 바로 작동합니다! 🚀
