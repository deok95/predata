#!/bin/bash
# stop-all.sh - 모든 MSA 서비스 종료

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}서비스를 종료합니다...${NC}"
echo ""

# PID 파일 기반으로 종료
for service in member-service question-service betting-service settlement-service; do
    if [ -f "/tmp/${service}.pid" ]; then
        pid=$(cat "/tmp/${service}.pid")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}Stopping $service (PID: $pid)...${NC}"
            kill $pid
            rm "/tmp/${service}.pid"
            echo -e "${GREEN}✅ $service stopped${NC}"
        else
            echo -e "${YELLOW}⚠️  $service was not running${NC}"
            rm "/tmp/${service}.pid"
        fi
    else
        echo -e "${YELLOW}⚠️  No PID file for $service${NC}"
    fi
done

# 추가로 gradlew 프로세스 정리
echo ""
echo -e "${YELLOW}Cleaning up remaining Gradle processes...${NC}"
pkill -f "gradlew.*bootRun" 2>/dev/null && echo -e "${GREEN}✅ Gradle processes killed${NC}" || echo -e "${YELLOW}⚠️  No Gradle processes found${NC}"

echo ""
echo -e "${GREEN}✅ 모든 서비스가 종료되었습니다${NC}"
