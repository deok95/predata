# Dotfiles

macOS 개발환경 자동 설정 스크립트

## 지원 머신

| 머신 | 프로필 | 특징 |
|------|--------|------|
| M1 Air 8GB | `8gb` | Docker/IntelliJ 제외, 메모리 최적화 |
| M1 Pro 16GB | `16gb` | 전체 개발환경 |

## 빠른 설치

```bash
# 1. 레포 클론
git clone <repo-url> ~/dotfiles
cd ~/dotfiles

# 2. 설치 실행 (메모리 자동 감지)
chmod +x install.sh
./install.sh

# 3. 터미널 재시작 또는
source ~/.zshrc
```

## 설치되는 도구

### 공통 (8GB / 16GB)
- **CLI**: git, node, python, gradle, maven
- **Java**: OpenJDK 17, Zulu 17
- **Apps**: Cursor, DBeaver, Postman, VS Code, Slack

### 16GB 전용
- Docker Desktop
- IntelliJ IDEA CE
- MariaDB, Redis (자동 시작)

## 파일 구조

```
dotfiles/
├── Brewfile           # 16GB용 패키지
├── Brewfile.8gb       # 8GB용 패키지 (경량)
├── install.sh         # 설치 스크립트
├── .zshrc             # 공통 쉘 설정
├── configs/
│   ├── memory-8gb.zshrc   # 8GB 메모리 최적화
│   ├── memory-16gb.zshrc  # 16GB 설정
│   └── db/
│       ├── my.cnf.8gb     # MariaDB 8GB (버퍼 256MB)
│       ├── my.cnf.16gb    # MariaDB 16GB (버퍼 1GB)
│       ├── redis.conf.8gb # Redis 8GB (256MB 제한)
│       └── redis.conf.16gb# Redis 16GB (512MB 제한)
└── README.md
```

## 8GB 메모리 최적화

8GB 머신에서 자동 적용되는 설정:

```bash
# Node.js 힙 제한 (2GB)
NODE_OPTIONS="--max-old-space-size=2048"

# Gradle 데몬 비활성화 + 힙 제한
GRADLE_OPTS="-Xmx1g -Dorg.gradle.daemon=false"

# Maven 힙 제한
MAVEN_OPTS="-Xmx1g"
```

## 유용한 명령어

### 공통
```bash
# 데이터베이스
db-start / db-stop      # MariaDB
redis-start / redis-stop # Redis

# Git 단축키
gs  # git status
gd  # git diff
gc  # git commit
gp  # git push
gl  # git log --oneline -10
```

### 8GB 전용
```bash
meminfo    # 메모리 사용량 상세
memfree    # 여유 메모리 확인
dev-db     # MariaDB 시작 (필요시에만)
dev-redis  # Redis 시작 (필요시에만)
```

### 16GB 전용
```bash
dev-start  # 모든 서비스 시작
dev-stop   # 모든 서비스 중지
docker-start # Docker Desktop 실행
```

## 수동 프로필 선택

자동 감지 대신 특정 프로필 사용:

```bash
# 8GB 프로필 강제 사용
brew bundle --file=Brewfile.8gb

# 16GB 프로필 강제 사용
brew bundle --file=Brewfile
```

## 기존 설정 복원

install.sh는 기존 `.zshrc`를 자동 백업합니다:
```bash
# 백업 파일 확인
ls ~/.zshrc.backup.*

# 복원
cp ~/.zshrc.backup.YYYYMMDD_HHMMSS ~/.zshrc
```

## 추가 설정 (수동)

### Docker Desktop (16GB)
1. Docker Desktop 실행
2. Settings > Resources > Memory: 4-6GB 권장

### VS Code 확장
Brewfile에서 자동 설치되지만, 수동 설치 시:
```bash
code --install-extension anthropic.claude-code
code --install-extension github.copilot
code --install-extension vscjava.vscode-java-pack
```
