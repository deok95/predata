#!/bin/bash
# Predata MSA 서비스 시작 스크립트

echo "🚀 Predata MSA 서비스를 시작합니다..."

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 서비스 시작 함수
start_service() {
    local service_name=$1
    local port=$2
    
    echo -e "${YELLOW}Starting $service_name on port $port...${NC}"
    cd /Users/mac/Desktop/predata/predata/services/$service_name
    ./gradlew bootRun &
    sleep 5
    echo -e "${GREEN}✅ $service_name started!${NC}"
}

# MariaDB 데이터베이스 생성
echo "📦 데이터베이스를 초기화합니다..."
mysql -u root -p1234 < /Users/mac/Desktop/predata/predata/init-db.sql

# 각 서비스 시작 (순서대로)
start_service "member-service" 8081
start_service "question-service" 8082
start_service "betting-service" 8083
start_service "settlement-service" 8084

echo ""
echo -e "${GREEN}🎉 모든 서비스가 시작되었습니다!${NC}"
echo ""
echo "서비스 URL:"
echo "  - Member Service:     http://localhost:8081"
echo "  - Question Service:   http://localhost:8082"
echo "  - Betting Service:    http://localhost:8083"
echo "  - Settlement Service: http://localhost:8084"
echo ""
echo "프론트엔드: http://localhost:3000"
