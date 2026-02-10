#!/bin/bash
# setup-env.sh - Environment setup helper for local development

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Predata Environment Setup           ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if .env.local exists
if [ ! -f "$PROJECT_DIR/.env.local" ]; then
    echo -e "${YELLOW}Creating .env.local from template...${NC}"
    cat > "$PROJECT_DIR/.env.local" <<EOF
# Local development environment variables
export DB_PASSWORD=predata
EOF
    echo -e "${GREEN}✅ Created .env.local${NC}"
else
    echo -e "${GREEN}✅ .env.local already exists${NC}"
fi

# Verify MariaDB is running
echo ""
echo -e "${YELLOW}Checking MariaDB...${NC}"
if pgrep -x "mariadbd" > /dev/null; then
    echo -e "${GREEN}✅ MariaDB is running${NC}"
else
    echo -e "${RED}❌ MariaDB is not running${NC}"
    echo "Start it with: brew services start mariadb"
    exit 1
fi

# Verify Redis is running
echo -e "${YELLOW}Checking Redis...${NC}"
if pgrep "redis-server" > /dev/null; then
    echo -e "${GREEN}✅ Redis is running${NC}"
else
    echo -e "${RED}❌ Redis is not running${NC}"
    echo "Start it with: brew services start redis"
    exit 1
fi

# Test database connection
echo ""
echo -e "${YELLOW}Testing database connection...${NC}"
source "$PROJECT_DIR/.env.local"
if mysql -u root -p${DB_PASSWORD} -e "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Database connection successful${NC}"
else
    echo -e "${RED}❌ Database connection failed${NC}"
    echo "Check your DB_PASSWORD in .env.local"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Environment is ready!                ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "You can now run: ./start-all.sh"
