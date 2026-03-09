-- V26: Rename buy_order_id/sell_order_id to taker_order_id/maker_order_id
-- Taker = 주문을 넣어서 체결을 일으킨 쪽
-- Maker = 오더북에 대기 중이던 쪽

SET @buy_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trades'
      AND COLUMN_NAME = 'buy_order_id'
);
SET @taker_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trades'
      AND COLUMN_NAME = 'taker_order_id'
);

SET @sql = IF(@buy_exists = 1 AND @taker_exists = 0,
    'ALTER TABLE trades RENAME COLUMN buy_order_id TO taker_order_id',
    'SELECT "Rename buy_order_id skipped" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sell_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trades'
      AND COLUMN_NAME = 'sell_order_id'
);
SET @maker_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trades'
      AND COLUMN_NAME = 'maker_order_id'
);

SET @sql = IF(@sell_exists = 1 AND @maker_exists = 0,
    'ALTER TABLE trades RENAME COLUMN sell_order_id TO maker_order_id',
    'SELECT "Rename sell_order_id skipped" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
