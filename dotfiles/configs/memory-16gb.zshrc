# memory-16gb.zshrc - Settings for 16GB+ Macs
# Automatically sourced by .zshrc when running on 16GB+ machines

# ============================================
# Node.js Settings
# ============================================
# Allow Node.js to use more memory for large projects
export NODE_OPTIONS="--max-old-space-size=4096"

# ============================================
# Gradle Settings
# ============================================
# Enable daemon for faster builds (uses ~500MB idle memory)
export GRADLE_OPTS="-Xmx2g"

# ============================================
# Maven Settings
# ============================================
export MAVEN_OPTS="-Xmx2g"

# ============================================
# Docker Desktop
# ============================================
# Docker memory is configured in Docker Desktop settings
# Recommended: 4-6GB for 16GB machines

# Alias to start Docker Desktop
alias docker-start="open -a Docker"

# ============================================
# Development Aliases
# ============================================
# Start all services for local development
alias dev-start="brew services start mariadb && brew services start redis && echo 'All services started.'"
alias dev-stop="brew services stop mariadb && brew services stop redis && echo 'All services stopped.'"

echo "16GB profile loaded."
