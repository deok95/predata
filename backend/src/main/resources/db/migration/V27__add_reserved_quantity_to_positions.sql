-- V27: Add reserved_quantity to positions for SELL oversell prevention
-- reserved_quantity = 미체결 SELL 주문으로 묶여있는 수량(예약)

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'positions'
      AND COLUMN_NAME = 'reserved_quantity'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE positions ADD COLUMN reserved_quantity DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT ''미체결 SELL 주문으로 예약된 수량'' AFTER quantity',
    'SELECT \"Column reserved_quantity already exists, skipping\" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

