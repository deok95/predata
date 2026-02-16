-- V21: vote_commits 테이블에서 salt 컬럼 제거
-- Commit-Reveal 보안 수정: salt는 클라이언트 전용으로 전환

-- 1. 기존 salt 컬럼 존재 여부 확인 및 DROP
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'vote_commits'
    AND COLUMN_NAME = 'salt'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE vote_commits DROP COLUMN salt',
    'SELECT "Column salt does not exist, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
