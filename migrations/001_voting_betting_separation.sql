-- Migration: Voting/Betting System Separation
-- Date: 2026-02-05
-- Description: Add new fields for voting/betting phase separation

USE predata;

-- 1. Add new columns with nullable first
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) DEFAULT NULL AFTER status,
    ADD COLUMN IF NOT EXISTS voting_end_at DATETIME DEFAULT NULL AFTER type,
    ADD COLUMN IF NOT EXISTS betting_start_at DATETIME DEFAULT NULL AFTER voting_end_at,
    ADD COLUMN IF NOT EXISTS betting_end_at DATETIME DEFAULT NULL AFTER betting_start_at;

-- 2. Set default values for type
UPDATE questions SET type = 'VERIFIABLE' WHERE type IS NULL;

-- 3. Set time fields based on existing expired_at
UPDATE questions SET voting_end_at = expired_at WHERE voting_end_at IS NULL;
UPDATE questions SET betting_start_at = expired_at WHERE betting_start_at IS NULL;
UPDATE questions SET betting_end_at = expired_at WHERE betting_end_at IS NULL;

-- 4. Update status values (OPEN -> VOTING, CLOSED -> BETTING, SETTLED -> SETTLED)
UPDATE questions SET status = 'VOTING' WHERE status = 'OPEN';
UPDATE questions SET status = 'BETTING' WHERE status = 'CLOSED';

-- 5. Make columns non-nullable now that they have values
ALTER TABLE questions
    MODIFY COLUMN type VARCHAR(20) NOT NULL,
    MODIFY COLUMN voting_end_at DATETIME NOT NULL,
    MODIFY COLUMN betting_start_at DATETIME NOT NULL,
    MODIFY COLUMN betting_end_at DATETIME NOT NULL;

-- 6. Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_questions_status ON questions(status);
CREATE INDEX IF NOT EXISTS idx_questions_type ON questions(type);
CREATE INDEX IF NOT EXISTS idx_questions_voting_end_at ON questions(voting_end_at);
CREATE INDEX IF NOT EXISTS idx_questions_betting_start_at ON questions(betting_start_at);
CREATE INDEX IF NOT EXISTS idx_questions_betting_end_at ON questions(betting_end_at);

-- 7. Verify migration
SELECT
    question_id,
    title,
    status,
    type,
    voting_end_at,
    betting_start_at,
    betting_end_at,
    expired_at,
    created_at
FROM questions
ORDER BY created_at DESC
LIMIT 10;
