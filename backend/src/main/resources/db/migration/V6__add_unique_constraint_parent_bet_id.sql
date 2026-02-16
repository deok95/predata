-- V6: parent_bet_id에 UNIQUE 제약조건 추가 (베팅 중복 판매 방지)

-- activities 테이블의 parent_bet_id 컬럼에 UNIQUE 제약조건 추가
-- NULL 값은 UNIQUE 제약조건에서 제외됨 (BET 타입은 parent_bet_id가 NULL)
-- Check if constraint exists before adding
SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activities'
    AND CONSTRAINT_NAME = 'uk_activities_parent_bet_id'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE activities ADD CONSTRAINT uk_activities_parent_bet_id UNIQUE (parent_bet_id)',
    'SELECT "Constraint uk_activities_parent_bet_id already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
