# Predata 블록체인 (Base L2)

## 📋 개요

Predata 예측 시장의 스마트 컨트랙트입니다. Base L2에 배포되어 투명성과 탈중앙화를 보장합니다.

## 🏗️ 아키텍처

```
온체인 (스마트 컨트랙트)
├─ 질문 정보 (ID, 제목, 카테고리)
├─ 베팅 풀 (YES/NO 금액)
├─ 사용자 베팅 (지갑 주소, 금액, 선택)
└─ 정산 결과

오프체인 (MariaDB)
├─ 페르소나 (나이, 직업, 국적)
├─ 지갑 ↔ 페르소나 매핑
└─ 투표 데이터 (5-Lock 티켓)
```

## 🚀 배포 방법

### 1. 환경 변수 설정

`.env` 파일 생성:

```bash
# Base Sepolia 테스트넷
BASE_SEPOLIA_RPC_URL=https://sepolia.base.org
PRIVATE_KEY=your_private_key_here
BASESCAN_API_KEY=your_basescan_api_key

# Base 메인넷 (나중에)
BASE_RPC_URL=https://mainnet.base.org
```

### 2. 컴파일

```bash
npx hardhat compile
```

### 3. 테스트넷 배포 (Base Sepolia)

```bash
npx hardhat run scripts/deploy.js --network baseSepolia
```

### 4. 메인넷 배포 (Base)

```bash
npx hardhat run scripts/deploy.js --network base
```

### 5. Basescan 검증

```bash
npx hardhat verify --network baseSepolia DEPLOYED_CONTRACT_ADDRESS
```

## 📊 가스비 예상

**Base Sepolia (테스트넷)**:
- 무료 (faucet에서 받기)
- Faucet: https://www.coinbase.com/faucets/base-ethereum-sepolia-faucet

**Base 메인넷**:
- 질문 생성: ~$0.01
- 베팅 배치 (100개): ~$0.05
- 정산: ~$0.01

**월 예상 비용** (하루 100개 질문, 10,000 베팅):
- 질문 생성: $30/월
- 베팅 (배치 100개씩): $60/월
- 정산: $30/월
- **총: ~$120/월** ✅

## 🔧 스마트 컨트랙트 기능

### createQuestion
관리자가 새로운 질문 생성

### batchPlaceBets
배치로 베팅 처리 (가스비 절감)

### settleQuestion
질문 정산

### claimWinnings
승자가 당첨금 청구 (이벤트만 발생, 실제 지급은 백엔드)

### calculateOdds
현재 배당률 계산

## 🔗 연동

백엔드(Spring Boot)에서 Web3j로 연동:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.web3j:core:4.9.8")
}
```

## ⚠️ 주의사항

1. **프라이빗 키 보안**
   - `.env`는 절대 커밋하지 말 것
   - 프로덕션은 AWS Secrets Manager 사용

2. **Admin 주소 관리**
   - 배포한 계정이 Admin
   - 모든 관리자 기능 호출 가능

3. **가스비 모니터링**
   - Base L2는 저렴하지만 모니터링 필요
   - 배치 크기 조정으로 최적화

## 📚 더 보기

- [Base 문서](https://docs.base.org)
- [Hardhat 문서](https://hardhat.org/docs)
- [Web3j 문서](https://docs.web3j.io)
