-- V31: Add sponsor and auto-resolution fields to questions table
-- Purpose: Support BRANDED category (sponsored questions) and AUTO resolution type

-- Add sponsor fields for BRANDED category questions
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'sponsor_name'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN sponsor_name VARCHAR(100) NULL COMMENT ''Sponsor name for BRANDED questions''',
    'SELECT "Column sponsor_name already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'sponsor_logo'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN sponsor_logo VARCHAR(500) NULL COMMENT ''Sponsor logo URL for BRANDED questions''',
    'SELECT "Column sponsor_logo already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add auto-resolution fields
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'resolution_type'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN resolution_type VARCHAR(20) NOT NULL DEFAULT ''MANUAL'' COMMENT ''MANUAL or AUTO settlement type''',
    'SELECT "Column resolution_type already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'resolution_config'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN resolution_config TEXT NULL COMMENT ''JSON config for auto-resolution (matchId, asset, condition, etc)''',
    'SELECT "Column resolution_config already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add indexes for efficient filtering
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND INDEX_NAME = 'idx_questions_resolution_type_status'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_questions_resolution_type_status ON questions(resolution_type, status)',
    'SELECT "Index idx_questions_resolution_type_status already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND INDEX_NAME = 'idx_questions_category'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_questions_category ON questions(category)',
    'SELECT "Index idx_questions_category already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Comments for context
-- resolution_type: 'MANUAL' (admin/vote-based settlement) or 'AUTO' (external data source)
-- resolution_config: JSON format examples:
--   Football: {"matchId":"12345"}
--   Price: {"asset":"BTC","condition":">=100000"}
-- Reuses existing resolutionSource field for source type (FOOTBALL_API, PRICE_API)
