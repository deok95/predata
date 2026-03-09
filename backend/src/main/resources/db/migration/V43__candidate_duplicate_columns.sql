-- V43: question_market_candidates 중복 제외 사유/기준 질문 저장 컬럼

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_market_candidates'
      AND COLUMN_NAME  = 'selection_reason'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE question_market_candidates ADD COLUMN selection_reason VARCHAR(30) NULL',
    'SELECT "Column selection_reason already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_market_candidates'
      AND COLUMN_NAME  = 'canonical_question_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE question_market_candidates ADD COLUMN canonical_question_id BIGINT NULL',
    'SELECT "Column canonical_question_id already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_market_candidates'
      AND INDEX_NAME   = 'idx_qmc_canonical_question_id'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_qmc_canonical_question_id ON question_market_candidates (canonical_question_id)',
    'SELECT "Index idx_qmc_canonical_question_id already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
