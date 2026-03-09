-- Add view_count column to questions table
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'view_count'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE questions ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0',
    'SELECT "Column view_count already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
