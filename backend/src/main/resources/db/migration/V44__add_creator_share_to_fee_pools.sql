-- V44: fee_pools에 생성자 수수료 몫 컬럼 추가

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fee_pools'
      AND COLUMN_NAME = 'creator_share'
);

SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE fee_pools ADD COLUMN creator_share DECIMAL(18, 6) NOT NULL DEFAULT 0.000000 AFTER platform_share',
    'SELECT "Column creator_share already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
