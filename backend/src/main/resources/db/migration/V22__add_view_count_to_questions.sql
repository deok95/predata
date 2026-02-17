-- Add view_count column to questions table
ALTER TABLE questions ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
