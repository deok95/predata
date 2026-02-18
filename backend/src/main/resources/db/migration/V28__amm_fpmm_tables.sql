-- AMM (FPMM) 실행 모델을 위한 테이블 생성

-- 1. market_pools 테이블: FPMM 풀 상태 관리
CREATE TABLE market_pools (
    question_id BIGINT PRIMARY KEY,
    yes_shares DECIMAL(38,18) NOT NULL,
    no_shares DECIMAL(38,18) NOT NULL,
    fee_rate DECIMAL(6,5) NOT NULL,
    collateral_locked DECIMAL(38,18) NOT NULL DEFAULT 0,
    total_volume_usdc DECIMAL(38,18) NOT NULL DEFAULT 0,
    total_fees_usdc DECIMAL(38,18) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_market_pools_question FOREIGN KEY (question_id)
        REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT chk_market_pools_status CHECK (status IN ('ACTIVE', 'PAUSED', 'SETTLED')),
    CONSTRAINT chk_market_pools_shares_positive CHECK (yes_shares > 0 AND no_shares > 0),
    CONSTRAINT chk_market_pools_fee_rate CHECK (fee_rate >= 0 AND fee_rate < 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_market_pools_status ON market_pools(status);

-- 2. user_shares 테이블: 사용자별 shares 보유량
CREATE TABLE user_shares (
    member_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    outcome VARCHAR(10) NOT NULL,
    shares DECIMAL(38,18) NOT NULL DEFAULT 0,
    cost_basis_usdc DECIMAL(38,18) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (member_id, question_id, outcome),

    CONSTRAINT fk_user_shares_member FOREIGN KEY (member_id)
        REFERENCES members(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_shares_question FOREIGN KEY (question_id)
        REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT chk_user_shares_outcome CHECK (outcome IN ('YES', 'NO')),
    CONSTRAINT chk_user_shares_shares_nonnegative CHECK (shares >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_shares_question_outcome ON user_shares(question_id, outcome);
CREATE INDEX idx_user_shares_member ON user_shares(member_id);

-- 3. swap_history 테이블: 스왑 거래 내역
CREATE TABLE swap_history (
    swap_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    action VARCHAR(10) NOT NULL,
    outcome VARCHAR(10) NOT NULL,
    usdc_in DECIMAL(38,18) NOT NULL,
    usdc_out DECIMAL(38,18) NOT NULL,
    shares_in DECIMAL(38,18) NOT NULL,
    shares_out DECIMAL(38,18) NOT NULL,
    fee_usdc DECIMAL(38,18) NOT NULL,
    price_before_yes DECIMAL(6,4) NOT NULL,
    price_after_yes DECIMAL(6,4) NOT NULL,
    yes_before DECIMAL(38,18) NOT NULL,
    no_before DECIMAL(38,18) NOT NULL,
    yes_after DECIMAL(38,18) NOT NULL,
    no_after DECIMAL(38,18) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_swap_history_member FOREIGN KEY (member_id)
        REFERENCES members(member_id) ON DELETE CASCADE,
    CONSTRAINT fk_swap_history_question FOREIGN KEY (question_id)
        REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT chk_swap_history_action CHECK (action IN ('BUY', 'SELL')),
    CONSTRAINT chk_swap_history_outcome CHECK (outcome IN ('YES', 'NO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_swap_history_question_created ON swap_history(question_id, created_at);
CREATE INDEX idx_swap_history_member_created ON swap_history(member_id, created_at);

-- 4. questions 테이블에 execution_model 컬럼 추가
ALTER TABLE questions
ADD COLUMN execution_model VARCHAR(20) NOT NULL DEFAULT 'ORDERBOOK_LEGACY'
AFTER phase;

ALTER TABLE questions
ADD CONSTRAINT chk_questions_execution_model
CHECK (execution_model IN ('AMM_FPMM', 'ORDERBOOK_LEGACY'));

CREATE INDEX idx_questions_execution_model ON questions(execution_model);
