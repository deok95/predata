-- Migration to update questions.status from VARCHAR to ENUM

-- Step 1: Update existing data to match new enum values
UPDATE questions SET status = 'VOTING' WHERE status = 'OPEN';
UPDATE questions SET status = 'SETTLED' WHERE status = 'CLOSED';

-- Step 2: Change column type to ENUM
ALTER TABLE questions
MODIFY COLUMN status ENUM('VOTING', 'BREAK', 'BETTING', 'SETTLED') NOT NULL DEFAULT 'VOTING';

-- Step 3: Update type column to ENUM as well
ALTER TABLE questions
MODIFY COLUMN type ENUM('VERIFIABLE', 'SUBJECTIVE') NOT NULL DEFAULT 'VERIFIABLE';
