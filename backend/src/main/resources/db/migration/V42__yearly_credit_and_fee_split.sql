-- V42: 연간 크레딧 모델 + 질문 수수료 분배정책 컬럼 추가

-- ── question_credit_accounts: 연간 예산/리셋 컬럼 ───────────────────────────
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_credit_accounts'
      AND COLUMN_NAME  = 'yearly_budget'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE question_credit_accounts ADD COLUMN yearly_budget INT NOT NULL DEFAULT 365',
    'SELECT "Column yearly_budget already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_credit_accounts'
      AND COLUMN_NAME  = 'last_reset_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE question_credit_accounts ADD COLUMN last_reset_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT "Column last_reset_at already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── questions: 수수료 분배정책 컬럼 ────────────────────────────────────────
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'platform_fee_share'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN platform_fee_share DECIMAL(5,4) NOT NULL DEFAULT 0.2000',
    'SELECT "Column platform_fee_share already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'creator_fee_share'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN creator_fee_share DECIMAL(5,4) NOT NULL DEFAULT 0.4000',
    'SELECT "Column creator_fee_share already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'voter_fee_share'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN voter_fee_share DECIMAL(5,4) NOT NULL DEFAULT 0.4000',
    'SELECT "Column voter_fee_share already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'creator_split_in_pool'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN creator_split_in_pool INT NOT NULL DEFAULT 50',
    'SELECT "Column creator_split_in_pool already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
