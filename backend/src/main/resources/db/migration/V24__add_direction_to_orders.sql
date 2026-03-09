-- Add direction column to orders table for BUY/SELL order model
-- BUY: 매수 주문 (USDC 예치)
-- SELL: 매도 주문 (포지션 담보)

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'direction'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE orders ADD COLUMN direction VARCHAR(4) NOT NULL DEFAULT ''BUY''',
    'SELECT "Column direction already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index for efficient filtering by direction
SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_direction'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_orders_direction ON orders(direction)',
    'SELECT "Index idx_orders_direction already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- All existing orders are BUY orders (기존 데이터는 모두 BUY)
-- DEFAULT 'BUY' handles this automatically
