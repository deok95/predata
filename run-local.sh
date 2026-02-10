#!/bin/bash
# run-local.sh - Docker 없이 로컬에서 Predata 실행
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Predata Local Development Server    ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 1. MariaDB 시작
echo -e "${YELLOW}[1/4] MariaDB 확인 중...${NC}"
if ! pgrep -x "mariadbd" > /dev/null; then
    echo "MariaDB 시작..."
    brew services start mariadb
    sleep 3
fi
echo -e "${GREEN}✅ MariaDB 실행 중${NC}"

# 2. Redis 시작
echo -e "${YELLOW}[2/4] Redis 확인 중...${NC}"
if ! pgrep -x "redis-server" > /dev/null; then
    echo "Redis 시작..."
    brew services start redis
    sleep 2
fi
echo -e "${GREEN}✅ Redis 실행 중${NC}"

# 3. 데이터베이스 초기화 (최초 1회)
echo -e "${YELLOW}[3/4] 데이터베이스 초기화...${NC}"
mariadb -u root < "$PROJECT_DIR/init-db.sql" 2>/dev/null || true
echo -e "${GREEN}✅ 데이터베이스 준비 완료${NC}"

# 4. 백엔드 시작
echo -e "${YELLOW}[4/4] 백엔드 서버 시작...${NC}"
cd "$PROJECT_DIR/backend"
./gradlew bootRun &
BACKEND_PID=$!

# 프론트엔드는 별도 터미널에서 실행하도록 안내
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   서버 시작 완료!                      ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "백엔드:    http://localhost:8080"
echo ""
echo -e "${YELLOW}프론트엔드 시작하려면 새 터미널에서:${NC}"
echo "  cd $PROJECT_DIR && npm run dev"
echo ""
echo -e "${YELLOW}서버 종료: Ctrl+C${NC}"

wait $BACKEND_PID
