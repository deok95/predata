#!/bin/bash
# Predata MSA 서비스 시작 스크립트

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 프로젝트 루트 디렉토리 자동 감지
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${GREEN}🚀 Predata MSA 서비스를 시작합니다...${NC}"
echo -e "${YELLOW}프로젝트 경로: $PROJECT_DIR${NC}"
echo ""

# 환경 변수 로드
if [ -f "$PROJECT_DIR/.env.local" ]; then
    echo -e "${YELLOW}환경 변수 로드 중...${NC}"
    source "$PROJECT_DIR/.env.local"
    echo -e "${GREEN}✅ 환경 변수 로드 완료${NC}"
else
    echo -e "${YELLOW}⚠️  .env.local 파일이 없습니다. 기본값을 사용합니다.${NC}"
    export DB_PASSWORD="${DB_PASSWORD:-predata}"
fi

# 서비스 시작 함수
start_service() {
    local service_name=$1
    local port=$2

    echo ""
    echo -e "${YELLOW}Starting $service_name on port $port...${NC}"
    cd "$PROJECT_DIR"

    # 백그라운드에서 실행하고 로그는 파일로
    ./gradlew :services:$service_name:bootRun > "/tmp/${service_name}.log" 2>&1 &
    local pid=$!
    echo "$pid" > "/tmp/${service_name}.pid"

    # 서비스 시작 대기
    sleep 5

    # 프로세스가 실행 중인지 확인
    if ps -p $pid > /dev/null; then
        echo -e "${GREEN}✅ $service_name started (PID: $pid)${NC}"
        echo -e "${YELLOW}   Log: /tmp/${service_name}.log${NC}"
    else
        echo -e "${RED}❌ $service_name failed to start${NC}"
        echo -e "${RED}   Last 20 lines of log:${NC}"
        tail -20 "/tmp/${service_name}.log"
        return 1
    fi
}

# MariaDB 확인
echo -e "${YELLOW}MariaDB 확인 중...${NC}"
if ! pgrep -x "mariadbd" > /dev/null; then
    echo -e "${RED}❌ MariaDB가 실행되지 않았습니다${NC}"
    echo "다음 명령어로 시작하세요: brew services start mariadb"
    exit 1
fi
echo -e "${GREEN}✅ MariaDB 실행 중${NC}"

# Redis 확인
echo -e "${YELLOW}Redis 확인 중...${NC}"
if ! pgrep "redis-server" > /dev/null; then
    echo -e "${RED}❌ Redis가 실행되지 않았습니다${NC}"
    echo "다음 명령어로 시작하세요: brew services start redis"
    exit 1
fi
echo -e "${GREEN}✅ Redis 실행 중${NC}"

# 데이터베이스 초기화
echo ""
echo -e "${YELLOW}📦 데이터베이스를 초기화합니다...${NC}"
# CREATE DATABASE 부분만 실행 (GRANT 오류 방지)
mysql -u root -p${DB_PASSWORD} -e "
CREATE DATABASE IF NOT EXISTS predata_member;
CREATE DATABASE IF NOT EXISTS predata_question;
CREATE DATABASE IF NOT EXISTS predata_betting;
CREATE DATABASE IF NOT EXISTS predata_settlement;
CREATE DATABASE IF NOT EXISTS predata_data;
CREATE DATABASE IF NOT EXISTS predata_sports;
CREATE DATABASE IF NOT EXISTS predata_blockchain;
" 2>/dev/null || true
echo -e "${GREEN}✅ 데이터베이스 초기화 완료${NC}"

# 각 서비스 시작 (순서대로)
start_service "member-service" 8081
start_service "question-service" 8082
start_service "betting-service" 8083
start_service "settlement-service" 8084

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 모든 서비스가 시작되었습니다!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "서비스 URL:"
echo "  - Member Service:     http://localhost:8081"
echo "  - Question Service:   http://localhost:8082"
echo "  - Betting Service:    http://localhost:8083"
echo "  - Settlement Service: http://localhost:8084"
echo ""
echo "프론트엔드: http://localhost:3000"
echo ""
echo -e "${YELLOW}로그 확인:${NC}"
echo "  tail -f /tmp/member-service.log"
echo "  tail -f /tmp/question-service.log"
echo "  tail -f /tmp/betting-service.log"
echo "  tail -f /tmp/settlement-service.log"
echo ""
echo -e "${YELLOW}서비스 종료:${NC}"
echo "  cd $PROJECT_DIR/services && ./stop-all.sh"
echo ""
