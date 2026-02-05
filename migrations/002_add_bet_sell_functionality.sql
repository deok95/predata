-- Migration: Add Bet Selling Functionality
-- Date: 2026-02-05
-- Description:
--   - Add BET_SELL to ActivityType enum
--   - Add parent_bet_id column for tracking sold bets
--   - Add index on parent_bet_id
--   - Remove unique constraint to allow multiple bets per question

-- 1. Add BET_SELL to ActivityType enum
ALTER TABLE activities
MODIFY COLUMN activity_type ENUM('VOTE', 'BET', 'BET_SELL') NOT NULL;

-- 2. Add parent_bet_id column
ALTER TABLE activities
ADD COLUMN parent_bet_id BIGINT NULL
COMMENT 'BET_SELL일 때 원본 베팅 ID 추적';

-- 3. Add index on parent_bet_id for faster lookups
ALTER TABLE activities
ADD INDEX idx_parent_bet_id (parent_bet_id);

-- 4. Drop unique constraint if exists (allows multiple bets per question)
-- Note: If the constraint doesn't exist, this will fail silently
-- Check constraint name first: SHOW CREATE TABLE activities;
-- Common constraint names: uk_member_question_type, uq_member_question_type
-- Uncomment and adjust the name if needed:
-- ALTER TABLE activities DROP INDEX uk_member_question_type;

-- 5. Optional: Add foreign key constraint (ensures referential integrity)
-- Uncomment if you want to enforce that parent_bet_id references a valid activity
-- ALTER TABLE activities
-- ADD CONSTRAINT fk_parent_bet
-- FOREIGN KEY (parent_bet_id) REFERENCES activities(activity_id)
-- ON DELETE SET NULL;

-- Verification queries (run after migration):
-- SELECT * FROM activities WHERE activity_type = 'BET_SELL' LIMIT 5;
-- SHOW INDEXES FROM activities WHERE Key_name = 'idx_parent_bet_id';
-- DESCRIBE activities;
