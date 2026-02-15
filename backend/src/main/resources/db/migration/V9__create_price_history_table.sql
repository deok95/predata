-- V9: PriceHistory 테이블 생성 (가격 이력)

-- Check if table already exists
SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'price_history'
);

-- Create table if it doesn't exist
SET @sql = IF(@table_exists = 0,
    'CREATE TABLE price_history (
        price_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        question_id BIGINT NOT NULL,
        mid_price DECIMAL(4, 2) COMMENT ''중간 가격 (bestBid + bestAsk) / 2'',
        last_trade_price DECIMAL(4, 2) COMMENT ''최근 체결가'',
        spread DECIMAL(4, 2) COMMENT ''스프레드 (bestAsk - bestBid)'',
        timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_price_history_question_id (question_id),
        INDEX idx_price_history_timestamp (question_id, timestamp)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''가격 이력''',
    'SELECT "Table price_history already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
