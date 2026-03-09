-- V45: 회원 지갑/원장/트레저리 원장 도입

CREATE TABLE IF NOT EXISTS member_wallets (
    wallet_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    available_balance DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    locked_balance DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_member_wallet_member FOREIGN KEY (member_id) REFERENCES members(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wallet_ledgers (
    ledger_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    direction VARCHAR(10) NOT NULL,
    tx_type VARCHAR(50) NOT NULL,
    amount DECIMAL(18, 6) NOT NULL,
    balance_after DECIMAL(18, 6) NOT NULL,
    locked_balance_after DECIMAL(18, 6) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wallet_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES member_wallets(wallet_id),
    CONSTRAINT fk_wallet_ledger_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    INDEX idx_wallet_ledger_member_created (member_id, created_at),
    INDEX idx_wallet_ledger_ref (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS treasury_ledgers (
    treasury_ledger_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_type VARCHAR(50) NOT NULL,
    amount DECIMAL(18, 6) NOT NULL,
    asset VARCHAR(10) NOT NULL DEFAULT 'USDC',
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_treasury_ledger_type_created (tx_type, created_at),
    INDEX idx_treasury_ledger_ref (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 회원 잔액을 member_wallets로 백필
INSERT INTO member_wallets (member_id, available_balance, locked_balance, version, created_at, updated_at)
SELECT m.member_id, COALESCE(m.usdc_balance, 0.000000), 0.000000, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM members m
LEFT JOIN member_wallets w ON w.member_id = m.member_id
WHERE w.member_id IS NULL;
