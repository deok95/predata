-- 수수료 풀 테이블
CREATE TABLE fee_pools (
    fee_pool_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL UNIQUE,
    total_fees DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    platform_share DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    reward_pool_share DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    reserve_share DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 수수료 풀 원장 테이블
CREATE TABLE fee_pool_ledgers (
    ledger_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fee_pool_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    amount DECIMAL(18, 6) NOT NULL,
    balance DECIMAL(18, 6) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (fee_pool_id) REFERENCES fee_pools(fee_pool_id) ON DELETE CASCADE,
    INDEX idx_fee_pool_ledger_fee_pool_id (fee_pool_id),
    INDEX idx_fee_pool_ledger_action (action),
    INDEX idx_fee_pool_ledger_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
