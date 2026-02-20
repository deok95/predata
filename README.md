# PRE(D)ATA

PRE(D)ATA는 투표자와 베팅자를 분리해 운영하는 예측 시장 플랫폼입니다.  
플랫폼 운영 과정에서 축적되는 결과를 B2B 인구통계 데이터로 활용하는 것이 핵심 목적입니다.  
현재 구조는 단일 백엔드 + 단일 프론트엔드 기준으로 운영됩니다.

## 1) 기술 스택

- Backend: Spring Boot 3.2.1 + Kotlin 1.9.22 + MariaDB
- Frontend: Next.js 16 + React 19 + TypeScript + TailwindCSS
- 인증: JWT (kid 기반 로테이션)
- 베팅 엔진: AMM (FPMM)
- 투표: Commit-Reveal
- 블록체인: Polygon (Web3j) - 결제/정산
- 캐시 서버: 사용 안 함

## 2) 필수 도구

- Java 17+
- Node.js 18+
- MariaDB 10.11+
- npm

## 3) Quick Start

### Step 1. 저장소 클론

```bash
git clone <YOUR_REPO_URL>
cd predata
```

### Step 2. MariaDB 설정

`init-db.sql` 사용:

```bash
mysql -u root -p < init-db.sql
```

또는 수동 생성:

```sql
CREATE DATABASE predata CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 3. 환경변수 설정

```bash
cp .env.example .env
```

최소 필수값:

```env
DB_PASSWORD=your_password
JWT_SECRET=your_generated_secret
AUTH_DEMO_MODE=true
```

`JWT_SECRET` 생성 예시:

```bash
openssl rand -hex 64
```

### Step 4. 백엔드 기동

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ../gradlew bootRun
```

- 백엔드 주소: http://localhost:8080
- Flyway가 시작 시 마이그레이션을 자동 적용합니다.

### Step 5. 프론트엔드 기동

프로젝트 루트(`predata/`)에서:

```bash
npm install
npm run dev
```

- 프론트 주소: http://localhost:3000

## 4) 프로젝트 구조

```text
predata/
├── src/            # Next.js 프론트엔드
├── backend/        # Spring Boot 백엔드
├── .env.example    # 환경변수 템플릿
├── init-db.sql     # DB 초기화
└── CLAUDE.md       # AI 개발 가이드
```

`gateway/`, `services/`는 비활성 아카이브이며 현재 실행 경로에서 사용하지 않습니다.

## 5) 테스트

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./gradlew test
```

## 6) 배포

- Backend: Mac Mini + Cloudflare Tunnel
- Frontend: Vercel

## 7) 환경변수 상세

전체 환경변수는 `.env.example`를 기준으로 관리합니다.

## 8) 주요 API 엔드포인트

- `POST /api/auth/send-code`
- `POST /api/swap`
- `POST /api/vote/commit`
- `GET /api/questions`
- `POST /api/admin/settle/{questionId}`

추가 API 목록은 `PREDATA_API_Collection.postman_collection.json`를 사용하세요.
