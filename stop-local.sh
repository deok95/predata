#!/bin/bash
# stop-local.sh - 로컬 서비스 종료

echo "서비스 종료 중..."
brew services stop mariadb
brew services stop redis
pkill -f "gradlew" 2>/dev/null || true
echo "✅ 모든 서비스가 종료되었습니다."
