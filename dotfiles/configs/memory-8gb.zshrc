# memory-8gb.zshrc - Memory optimization for 8GB Macs
# Automatically sourced by .zshrc when running on 8GB machines

# ============================================
# Node.js Memory Limits
# ============================================
# Limit Node.js heap to 2GB (default is ~4GB which can cause swapping)
export NODE_OPTIONS="--max-old-space-size=2048"

# ============================================
# Gradle Memory Limits
# ============================================
# Disable Gradle daemon (saves ~500MB idle memory)
# Limit JVM heap to 1GB
export GRADLE_OPTS="-Xmx1g -Dorg.gradle.daemon=false"

# ============================================
# Maven Memory Limits
# ============================================
export MAVEN_OPTS="-Xmx1g"

# ============================================
# VS Code Settings
# ============================================
# Reduce VS Code memory usage
export ELECTRON_ENABLE_LOGGING=0

# ============================================
# Helpful Aliases for 8GB Macs
# ============================================
# Show memory usage
alias meminfo="vm_stat | perl -ne '/page size of (\d+)/ and \$size=\$1; /Pages\s+([^:]+)[^\d]+(\d+)/ and printf(\"%s: %.2f GB\n\", \$1, \$2 * \$size / 1073741824);'"

# Kill memory-hungry processes
alias killchrome="pkill -f 'Google Chrome Helper'"
alias killdocker="pkill -f 'Docker'"

# Start databases only when needed
alias dev-db="brew services start mariadb && echo 'MariaDB started. Stop with: brew services stop mariadb'"
alias dev-redis="brew services start redis && echo 'Redis started. Stop with: brew services stop redis'"

# Quick memory check
alias memfree="echo 'Free Memory:' && vm_stat | grep 'Pages free' | awk '{print \$3 * 4096 / 1024 / 1024 \" MB\"}'"

echo "8GB memory optimizations loaded."
