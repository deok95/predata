-- Auto question generation foundation
-- 1) questions.vote_result_settlement flag
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS vote_result_settlement BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE questions
SET vote_result_settlement = CASE
    WHEN market_type = 'OPINION' THEN TRUE
    ELSE vote_result_settlement
END;

-- 2) trend signals (google trends source)
CREATE TABLE IF NOT EXISTS trend_signals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    signal_date DATE NOT NULL,
    subcategory VARCHAR(50) NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    trend_score INT NOT NULL,
    region VARCHAR(10) NOT NULL DEFAULT 'US',
    source VARCHAR(50) NOT NULL DEFAULT 'GOOGLE_TRENDS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_trend_signals_date_subcategory ON trend_signals(signal_date, subcategory);

-- 3) generation batches
CREATE TABLE IF NOT EXISTS question_generation_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    subcategory VARCHAR(50) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_count INT NOT NULL DEFAULT 3,
    accepted_count INT NOT NULL DEFAULT 0,
    rejected_count INT NOT NULL DEFAULT 0,
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_generation_batch_id (batch_id),
    UNIQUE KEY uk_generation_batch_daily (subcategory, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_generation_batches_status_created ON question_generation_batches(status, created_at);

-- 4) generation items (drafts)
CREATE TABLE IF NOT EXISTS question_generation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64) NOT NULL,
    title TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50) NOT NULL,
    market_type VARCHAR(20) NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    vote_result_settlement BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_rule TEXT NOT NULL,
    resolution_source VARCHAR(500) NULL,
    resolve_at DATETIME NOT NULL,
    voting_end_at DATETIME NOT NULL,
    break_minutes INT NOT NULL,
    betting_start_at DATETIME NOT NULL,
    betting_end_at DATETIME NOT NULL,
    reveal_start_at DATETIME NOT NULL,
    reveal_end_at DATETIME NOT NULL,
    confidence DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    duplicate_score DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    rationale TEXT NOT NULL,
    references_json TEXT NULL,
    risk_flags_json TEXT NULL,
    status VARCHAR(20) NOT NULL,
    reject_reason TEXT NULL,
    published_question_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_generation_item_draft (draft_id),
    CONSTRAINT fk_generation_item_batch FOREIGN KEY (batch_id) REFERENCES question_generation_batches(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_generation_items_batch_status ON question_generation_items(batch_id, status);
CREATE INDEX IF NOT EXISTS idx_generation_items_published ON question_generation_items(published_question_id);
