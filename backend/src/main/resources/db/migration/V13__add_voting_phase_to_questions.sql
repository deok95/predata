-- V13: voting_phase 컬럼 추가 (투표 단계 관리)

-- Check if column already exists
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'questions'
    AND COLUMN_NAME = 'voting_phase'
);

-- Add column if it doesn't exist
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE questions ADD COLUMN voting_phase VARCHAR(30) NOT NULL DEFAULT ''VOTING_COMMIT_OPEN'' COMMENT ''투표 단계 (VOTING_COMMIT_OPEN, VOTING_REVEAL_OPEN, etc.)''',
    'SELECT "Column voting_phase already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index if column was added
SET @index_sql = IF(@column_exists = 0,
    'CREATE INDEX idx_questions_voting_phase ON questions(voting_phase)',
    'SELECT "Index already exists or column not added" as message'
);

PREPARE stmt FROM @index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
