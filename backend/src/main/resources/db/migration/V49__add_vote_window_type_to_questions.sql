-- V49: add vote_window_type column to questions table
-- Backfill from voting duration (votingEndAt - createdAt)

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'vote_window_type'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN vote_window_type VARCHAR(10) NULL',
    'SELECT "Column vote_window_type already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill existing rows from timing:
-- <= 7 hours → H6, <= 25 hours → D1, else → D3
UPDATE questions
SET vote_window_type = CASE
    WHEN TIMESTAMPDIFF(HOUR, created_at, voting_end_at) <= 7  THEN 'H6'
    WHEN TIMESTAMPDIFF(HOUR, created_at, voting_end_at) <= 25 THEN 'D1'
    ELSE 'D3'
END
WHERE vote_window_type IS NULL;
