-- V3: USDC 결제 시스템 - pointBalance → usdcBalance 전환 + 결제 트랜잭션 테이블

-- 1. members 테이블의 point_balance → usdc_balance 변환
-- point_balance 가 존재하고 usdc_balance 가 아직 없을 때만 CHANGE COLUMN 수행
SET @point_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'members'
    AND COLUMN_NAME = 'point_balance'
);

SET @usdc_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'members'
    AND COLUMN_NAME = 'usdc_balance'
);

SET @sql = IF(@point_exists > 0 AND @usdc_exists = 0,
    'ALTER TABLE members CHANGE COLUMN point_balance usdc_balance DECIMAL(18,6) NOT NULL DEFAULT 0.000000',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 결제 트랜잭션 테이블 생성
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    tx_hash VARCHAR(66) NOT NULL,
    amount DECIMAL(18,6) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'TICKET_PURCHASE or DEPOSIT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, CONFIRMED, FAILED',
    quantity INT DEFAULT NULL COMMENT '티켓 수량 (TICKET_PURCHASE인 경우)',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE KEY uk_tx_hash (tx_hash),
    INDEX idx_member_payments (member_id, created_at),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
