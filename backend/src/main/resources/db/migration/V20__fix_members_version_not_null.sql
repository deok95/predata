-- V20: Fix members.version for optimistic locking
-- Problem: members.version was nullable, causing Hibernate NPE on @Version increment during updates.
-- Strategy:
-- 1) Backfill NULL versions to 0
-- 2) Enforce NOT NULL + DEFAULT 0 at schema level

UPDATE members
SET version = 0
WHERE version IS NULL;

ALTER TABLE members
MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;

