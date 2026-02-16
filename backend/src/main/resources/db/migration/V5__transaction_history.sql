-- V5: Transaction History - records all USDC balance changes for audit trail

CREATE TABLE IF NOT EXISTS transaction_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'DEPOSIT, WITHDRAW, BET, SETTLEMENT, VOTING_PASS',
    amount DECIMAL(18,6) NOT NULL COMMENT 'Signed: positive=credit, negative=debit',
    balance_after DECIMAL(18,6) NOT NULL COMMENT 'Member balance after this transaction',
    description VARCHAR(255) NOT NULL,
    question_id BIGINT DEFAULT NULL COMMENT 'Related question (nullable)',
    tx_hash VARCHAR(66) DEFAULT NULL COMMENT 'Blockchain tx hash (nullable)',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    INDEX idx_th_member (member_id, created_at DESC),
    INDEX idx_th_member_type (member_id, type, created_at DESC),
    CONSTRAINT fk_th_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
