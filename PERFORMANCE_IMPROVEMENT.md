# 동시 접속 300명 대응 성능 개선 방안

## 🎯 목표
- 동시 접속 300명 처리
- 에러율 < 1%
- 평균 응답 시간 < 500ms

---

## ⚡ 즉시 적용 가능한 개선 (Quick Wins)

### 1. 데이터베이스 커넥션 풀 확장
**위치:** `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50        # 10 → 50
      minimum-idle: 20              # 유휴 커넥션 최소값
      connection-timeout: 10000     # 10초
      idle-timeout: 300000          # 5분
      max-lifetime: 1800000         # 30분
      leak-detection-threshold: 60000  # 커넥션 누수 감지
```

**예상 효과:**
- 커넥션 대기 시간 80% 감소
- 타임아웃 에러 90% 감소

---

### 2. 낙관적 락 재시도 로직 추가
**위치:** `backend/src/main/kotlin/com/predata/backend/service/amm/SwapService.kt`

```kotlin
import org.springframework.retry.annotation.Retryable
import org.springframework.retry.annotation.Backoff
import jakarta.persistence.OptimisticLockException

@Retryable(
    value = [OptimisticLockException::class],
    maxAttempts = 5,
    backoff = Backoff(delay = 50, multiplier = 1.5)
)
@Transactional
fun executeSwap(memberId: Long, request: SwapRequest): SwapResponse {
    // 기존 코드 유지
    // 충돌 시 자동으로 최대 5회 재시도 (50ms → 75ms → 112ms ...)
}
```

**추가 설정:** `@EnableRetry` 어노테이션 필요
```kotlin
@Configuration
@EnableRetry
class RetryConfig
```

**예상 효과:**
- 낙관적 락 충돌 성공률 95%+ (5회 재시도)
- 사용자 에러 경험 감소

---

### 3. 트랜잭션 타임아웃 설정
```kotlin
@Transactional(timeout = 5)  // 5초
fun executeSwap(memberId: Long, request: SwapRequest): SwapResponse {
    // 5초 이상 걸리면 자동 롤백
}
```

**예상 효과:**
- 데드락 방지
- 커넥션 장시간 점유 방지

---

### 4. 스레드 풀 최적화
**위치:** `application.yml`

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 20
        max-size: 100
        queue-capacity: 500
```

---

## 🚀 중기 개선 (1-2주 소요)

### 5. 읽기/쓰기 분리 (Read Replica)
- Master DB: 쓰기 전용
- Slave DB: 읽기 전용 (poolState 조회 등)
- 쓰기 부하 50% 감소

```kotlin
@Transactional(readOnly = true)
fun getPoolState(questionId: Long): PoolStateResponse {
    // Slave DB에서 읽기
}
```

---

### 6. Redis 캐싱 추가
```kotlin
@Cacheable(value = ["poolState"], key = "#questionId")
@Transactional(readOnly = true)
fun getPoolState(questionId: Long): PoolStateResponse {
    // 캐시 미스 시에만 DB 조회
}
```

**캐시 무효화:**
```kotlin
@CacheEvict(value = ["poolState"], key = "#request.questionId")
fun executeSwap(...) {
    // 스왑 후 캐시 삭제
}
```

**예상 효과:**
- 읽기 쿼리 90% 감소
- DB 부하 대폭 감소

---

## 🏗️ 장기 개선 (1-2개월 소요)

### 7. 메시지 큐 도입 (비동기 처리)
```
User → API Gateway → Kafka → Swap Worker (여러 인스턴스)
                        ↓
                   MarketPool DB
```

**장점:**
- 스파이크 트래픽 흡수
- 수평 확장 가능
- 순서 보장 (파티션별)

**단점:**
- 즉시 응답 불가 (비동기)
- 복잡도 증가

---

### 8. 비관적 락으로 전환 (선택적)
```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findByIdWithLock(questionId: Long): Optional<MarketPool>
```

**장점:**
- 재시도 불필요 (한 번에 성공)
- 충돌 완전 방지

**단점:**
- 락 대기 시간 증가
- 데드락 위험

---

## 📊 개선 후 예상 성능

| 항목 | 개선 전 | 개선 후 |
|-----|--------|---------|
| 동시 처리 | ~30명 | **300명+** |
| 에러율 | 80-90% | **< 1%** |
| 평균 응답 시간 | 10초+ | **< 500ms** |
| 커넥션 풀 | 10개 | **50개** |
| 재시도 성공률 | 0% | **95%+** |

---

## 🛠️ 우선순위 로드맵

### Phase 1 (즉시 - 1일)
1. ✅ 커넥션 풀 확장 (50개)
2. ✅ 재시도 로직 추가
3. ✅ 트랜잭션 타임아웃

### Phase 2 (1주)
4. ⬜ 스레드 풀 설정
5. ⬜ 모니터링 추가 (Prometheus + Grafana)

### Phase 3 (2주)
6. ⬜ Redis 캐싱
7. ⬜ Read Replica

### Phase 4 (1-2개월)
8. ⬜ Kafka 도입 (선택적)

---

## 🔍 모니터링 추가 (필수)

### 메트릭 수집
```kotlin
@Service
class SwapService(
    private val meterRegistry: MeterRegistry
) {
    fun executeSwap(...): SwapResponse {
        val timer = Timer.start(meterRegistry)
        try {
            // 스왑 로직
            meterRegistry.counter("swap.success").increment()
        } catch (e: Exception) {
            meterRegistry.counter("swap.error", "type", e.javaClass.simpleName).increment()
            throw e
        } finally {
            timer.stop(meterRegistry.timer("swap.duration"))
        }
    }
}
```

### 알람 설정
- 에러율 > 5% → Slack 알림
- 응답 시간 > 2초 → Slack 알림
- 커넥션 풀 사용률 > 80% → Slack 알림

---

## 📝 참고 자료
- [Spring Boot HikariCP 튜닝](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [JPA Optimistic Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Spring Retry](https://docs.spring.io/spring-retry/docs/current/reference/html/)
