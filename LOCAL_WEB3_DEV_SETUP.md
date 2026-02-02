# Predata 로컬 Web3 개발 환경 설정 가이드

## 🎯 개요

실제 Base L2 대신 **로컬 Hardhat 네트워크**를 사용하여 개발합니다.

## 📋 설정 단계

### 1️⃣ 로컬 블록체인 시작

```bash
cd blockchain
npx hardhat node
```

이렇게 하면:
- 로컬 블록체인이 `http://localhost:8545`에서 실행
- 10개의 테스트 계정 자동 생성 (각 10,000 ETH)
- 가스비 무료
- 즉시 블록 생성

### 2️⃣ 컨트랙트 배포 (로컬)

**새 터미널**에서:

```bash
cd blockchain
npx hardhat run scripts/deploy.js --network localhost
```

배포 후 **컨트랙트 주소**가 출력됩니다:
```
✅ PredataMarket 배포 완료!
📝 컨트랙트 주소: 0x5FbDB2315678afecb367f032d93F642f64180aa3
```

### 3️⃣ 백엔드 설정

`backend/src/main/resources/application-local.yml`:

```yaml
blockchain:
  enabled: true
  rpc:
    url: http://localhost:8545  # 로컬 Hardhat
  contract:
    address: "0x5FbDB2315678afecb367f032d93F642f64180aa3"  # 배포된 주소
  admin:
    private-key: "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"  # Hardhat 첫 번째 계정
```

### 4️⃣ 프론트엔드 설정

`src/lib/wagmi.ts`:

```typescript
import { getDefaultConfig } from '@rainbow-me/rainbowkit';
import { hardhat } from 'wagmi/chains';  // 로컬 체인 추가

export const config = getDefaultConfig({
  appName: 'Predata',
  projectId: 'demo',  // 데모용
  chains: [
    hardhat,  // 로컬 개발용
  ],
  ssr: true,
});
```

### 5️⃣ MetaMask 설정

1. MetaMask 열기
2. 네트워크 추가
3. 수동 추가:
   - **네트워크 이름**: Hardhat Local
   - **RPC URL**: http://localhost:8545
   - **체인 ID**: 31337
   - **통화 기호**: ETH

4. 테스트 계정 추가 (Hardhat이 제공):
   ```
   계정 #0: 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
   프라이빗 키: 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
   ```

---

## 🚀 실행 순서

```bash
# 터미널 1: 로컬 블록체인
cd blockchain
npx hardhat node

# 터미널 2: 컨트랙트 배포
cd blockchain
npx hardhat run scripts/deploy.js --network localhost
# 출력된 컨트랙트 주소를 복사

# 터미널 3: 백엔드
cd backend
# application-local.yml에 컨트랙트 주소 설정
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 터미널 4: 프론트엔드
npm run dev
```

---

## ✅ 장점

### 개발 환경
- ✅ **완전 무료** - 가스비 없음
- ✅ **빠른 테스트** - 즉시 블록 생성
- ✅ **디버깅 쉬움** - 콘솔 로그 확인 가능
- ✅ **재시작 가능** - 언제든 리셋

### 실제 환경과 동일
- ✅ 같은 Solidity 코드
- ✅ 같은 Web3 라이브러리
- ✅ 같은 트랜잭션 흐름
- ✅ 배포 시 코드 변경 없음

---

## 🔄 나중에 실제 네트워크로 전환

### Base Sepolia 테스트넷
```yaml
blockchain:
  enabled: true
  rpc:
    url: https://sepolia.base.org
  contract:
    address: "0x..."  # 실제 배포된 주소
```

### Base 메인넷
```yaml
blockchain:
  enabled: true
  rpc:
    url: https://mainnet.base.org
  contract:
    address: "0x..."  # 실제 배포된 주소
```

---

## 🎯 데모 시나리오

1. **로컬 블록체인 시작**
2. **컨트랙트 배포**
3. **백엔드 연결**
4. **프론트엔드에서 지갑 연결** (Hardhat 네트워크)
5. **질문 생성** → 온체인 기록
6. **베팅** → 배치로 온체인 전송
7. **정산** → 결과 온체인 기록
8. **검증** → 블록체인 탐색기에서 확인

---

## 💡 꿀팁

### Hardhat 콘솔에서 확인
```bash
npx hardhat console --network localhost

# 컨트랙트 조회
const Market = await ethers.getContractFactory("PredataMarket");
const market = await Market.attach("0x5FbDB...");
const question = await market.questions(1);
console.log(question);
```

### 리셋하기
Hardhat 노드를 **Ctrl+C**로 종료하고 다시 시작하면 완전히 리셋됩니다!

---

## 🎊 완성!

이제 완전히 로컬에서 Web3 기능을 테스트할 수 있습니다!

실제 배포는 데모가 완성된 후 Base L2로 이전하면 됩니다! 🚀
