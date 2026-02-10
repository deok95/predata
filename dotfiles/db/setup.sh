#!/bin/bash
# DB Setup Script - Creates databases and tables
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${YELLOW}Database Setup${NC}"
echo ""

# Check if MariaDB is running
if ! pgrep -x "mariadbd" > /dev/null; then
    echo "Starting MariaDB..."
    brew services start mariadb
    sleep 3
fi

# Get password
read -sp "MariaDB root password (press Enter if none): " DB_PASSWORD
echo ""

# Build mysql command
if [ -z "$DB_PASSWORD" ]; then
    MYSQL_CMD="mariadb -u root"
else
    MYSQL_CMD="mariadb -u root -p${DB_PASSWORD}"
fi

# Test connection
if ! $MYSQL_CMD -e "SELECT 1" &>/dev/null; then
    echo -e "${RED}Failed to connect to MariaDB${NC}"
    exit 1
fi

echo -e "${GREEN}Connected to MariaDB${NC}"
echo ""

# Import schema
echo "Importing schema..."
$MYSQL_CMD < "$SCRIPT_DIR/schema.sql"

echo -e "${GREEN}Database setup complete!${NC}"
echo ""
echo "Created databases:"
$MYSQL_CMD -e "SHOW DATABASES LIKE 'predata%';"
