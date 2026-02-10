# .zshrc - Shared ZSH Configuration
# Managed by dotfiles

# ============================================
# Homebrew (Apple Silicon)
# ============================================
eval "$(/opt/homebrew/bin/brew shellenv)"

# ============================================
# Java Configuration
# ============================================
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# ============================================
# Node.js / npm
# ============================================
export PATH="/opt/homebrew/opt/node/bin:$PATH"

# ============================================
# Python
# ============================================
export PATH="/opt/homebrew/opt/python@3.14/bin:$PATH"
alias python="python3"
alias pip="pip3"

# ============================================
# Aliases
# ============================================
alias ll="ls -la"
alias gs="git status"
alias gd="git diff"
alias gc="git commit"
alias gp="git push"
alias gl="git log --oneline -10"

# Database shortcuts (start manually when needed)
alias db-start="brew services start mariadb"
alias db-stop="brew services stop mariadb"
alias redis-start="brew services start redis"
alias redis-stop="brew services stop redis"

# ============================================
# History
# ============================================
HISTSIZE=10000
SAVEHIST=10000
HISTFILE=~/.zsh_history
setopt SHARE_HISTORY
setopt HIST_IGNORE_DUPS
setopt HIST_IGNORE_SPACE

# ============================================
# Prompt (simple and fast)
# ============================================
autoload -Uz vcs_info
precmd() { vcs_info }
zstyle ':vcs_info:git:*' formats ' (%b)'
setopt PROMPT_SUBST
PROMPT='%F{cyan}%1~%f%F{yellow}${vcs_info_msg_0_}%f %F{green}>%f '

# ============================================
# Completion
# ============================================
autoload -Uz compinit
compinit -C

# ============================================
# Memory-specific settings will be sourced below
# (Added by install.sh based on detected memory)
# ============================================
