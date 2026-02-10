#!/bin/bash
# install.sh - Automated development environment setup for macOS
# Supports: M1 Air 8GB, M1 Pro 16GB
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

DOTFILES_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   macOS Development Environment Setup  ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Detect system memory
get_memory_gb() {
    local memory_bytes=$(sysctl -n hw.memsize)
    echo $((memory_bytes / 1024 / 1024 / 1024))
}

MEMORY_GB=$(get_memory_gb)
echo -e "${GREEN}Detected Memory:${NC} ${MEMORY_GB}GB"

# Determine which profile to use
if [ "$MEMORY_GB" -le 8 ]; then
    PROFILE="8gb"
    BREWFILE="Brewfile.8gb"
    echo -e "${YELLOW}Using lightweight profile for 8GB Mac${NC}"
else
    PROFILE="16gb"
    BREWFILE="Brewfile"
    echo -e "${GREEN}Using full profile for 16GB+ Mac${NC}"
fi
echo ""

# Step 1: Xcode Command Line Tools
echo -e "${BLUE}[1/6] Checking Xcode Command Line Tools...${NC}"
if ! xcode-select -p &>/dev/null; then
    echo "Installing Xcode Command Line Tools..."
    xcode-select --install
    echo -e "${YELLOW}Please complete the installation and run this script again.${NC}"
    exit 1
else
    echo -e "${GREEN}Already installed.${NC}"
fi
echo ""

# Step 2: Homebrew
echo -e "${BLUE}[2/6] Checking Homebrew...${NC}"
if ! command -v brew &>/dev/null; then
    echo "Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

    # Add Homebrew to PATH for Apple Silicon
    echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
    eval "$(/opt/homebrew/bin/brew shellenv)"
else
    echo -e "${GREEN}Already installed.${NC}"
    brew update
fi
echo ""

# Step 3: Install packages via Brewfile
echo -e "${BLUE}[3/6] Installing packages from ${BREWFILE}...${NC}"
if [ -f "$DOTFILES_DIR/$BREWFILE" ]; then
    brew bundle --file="$DOTFILES_DIR/$BREWFILE" --no-lock
    echo -e "${GREEN}Packages installed successfully.${NC}"
else
    echo -e "${RED}Error: ${BREWFILE} not found!${NC}"
    exit 1
fi
echo ""

# Step 4: Setup .zshrc
echo -e "${BLUE}[4/6] Setting up .zshrc...${NC}"
ZSHRC_SOURCE="$DOTFILES_DIR/.zshrc"
ZSHRC_TARGET="$HOME/.zshrc"

# Backup existing .zshrc if it exists and is not a symlink
if [ -f "$ZSHRC_TARGET" ] && [ ! -L "$ZSHRC_TARGET" ]; then
    BACKUP_FILE="$HOME/.zshrc.backup.$(date +%Y%m%d_%H%M%S)"
    echo -e "${YELLOW}Backing up existing .zshrc to ${BACKUP_FILE}${NC}"
    mv "$ZSHRC_TARGET" "$BACKUP_FILE"
fi

# Create symlink
ln -sf "$ZSHRC_SOURCE" "$ZSHRC_TARGET"
echo -e "${GREEN}.zshrc linked successfully.${NC}"
echo ""

# Step 5: Apply memory-specific settings
echo -e "${BLUE}[5/6] Applying memory-specific settings...${NC}"
MEMORY_CONFIG="$DOTFILES_DIR/configs/memory-${PROFILE}.zshrc"
if [ -f "$MEMORY_CONFIG" ]; then
    # Append memory config source to .zshrc if not already present
    if ! grep -q "memory-${PROFILE}.zshrc" "$ZSHRC_SOURCE"; then
        echo "" >> "$ZSHRC_SOURCE"
        echo "# Memory-specific settings" >> "$ZSHRC_SOURCE"
        echo "source \"$MEMORY_CONFIG\"" >> "$ZSHRC_SOURCE"
    fi
    echo -e "${GREEN}Memory optimization settings applied for ${PROFILE} profile.${NC}"
else
    echo -e "${YELLOW}No memory-specific config found, skipping.${NC}"
fi
echo ""

# Step 6: Database configuration
echo -e "${BLUE}[6/7] Configuring databases...${NC}"
DB_CONFIG_DIR="$DOTFILES_DIR/configs/db"

# MariaDB config
if [ -f "$DB_CONFIG_DIR/my.cnf.${PROFILE}" ]; then
    if [ -f "/opt/homebrew/etc/my.cnf" ]; then
        cp /opt/homebrew/etc/my.cnf /opt/homebrew/etc/my.cnf.backup.$(date +%Y%m%d_%H%M%S)
    fi
    cp "$DB_CONFIG_DIR/my.cnf.${PROFILE}" /opt/homebrew/etc/my.cnf
    echo -e "${GREEN}MariaDB configured for ${PROFILE} profile.${NC}"
fi

# Redis config
if [ -f "$DB_CONFIG_DIR/redis.conf.${PROFILE}" ]; then
    if [ -f "/opt/homebrew/etc/redis.conf" ]; then
        cp /opt/homebrew/etc/redis.conf /opt/homebrew/etc/redis.conf.backup.$(date +%Y%m%d_%H%M%S)
    fi
    cp "$DB_CONFIG_DIR/redis.conf.${PROFILE}" /opt/homebrew/etc/redis.conf
    echo -e "${GREEN}Redis configured for ${PROFILE} profile.${NC}"
fi
echo ""

# Step 7: Post-installation tasks
echo -e "${BLUE}[7/7] Post-installation tasks...${NC}"

# Setup Java
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
    sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk 2>/dev/null || true
    echo -e "${GREEN}Java 17 configured.${NC}"
fi

# Disable auto-start for databases on 8GB machines
if [ "$PROFILE" = "8gb" ]; then
    echo -e "${YELLOW}Disabling auto-start for databases (8GB optimization)...${NC}"
    brew services stop mariadb 2>/dev/null || true
    brew services stop redis 2>/dev/null || true
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Installation Complete!               ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Profile: ${BLUE}${PROFILE}${NC}"
echo -e "Memory:  ${BLUE}${MEMORY_GB}GB${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Restart your terminal or run: source ~/.zshrc"
echo "  2. Verify installation:"
echo "     - java -version"
echo "     - node -v"
echo "     - python3 --version"
if [ "$PROFILE" = "16gb" ]; then
    echo "  3. Start Docker Desktop from Applications"
fi
echo ""
