-- V8: Position 테이블 생성 (포지션 원장)

-- Check if table already exists
SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'positions'
);

-- Create table if it doesn't exist
SET @sql = IF(@table_exists = 0,
    'CREATE TABLE positions (
        position_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        member_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        side VARCHAR(10) NOT NULL COMMENT ''YES or NO'',
        quantity DECIMAL(19, 2) NOT NULL COMMENT ''포지션 수량'',
        avg_price DECIMAL(4, 2) NOT NULL COMMENT ''평균 매수가'',
        version BIGINT NOT NULL DEFAULT 0 COMMENT ''낙관적 락 버전'',
        settled BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''정산 완료 여부'',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        CONSTRAINT uk_position_member_question_side UNIQUE (member_id, question_id, side),
        INDEX idx_positions_member_id (member_id),
        INDEX idx_positions_question_id (question_id),
        INDEX idx_positions_settled (question_id, settled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''포지션 원장''',
    'SELECT "Table positions already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
