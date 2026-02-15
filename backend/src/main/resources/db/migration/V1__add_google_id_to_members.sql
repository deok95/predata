-- Add google_id column to members table for Google OAuth integration
-- Migration: V1__add_google_id_to_members.sql
-- Date: 2026-02-09

ALTER TABLE members
ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE NULL
AFTER email;

-- Add index for faster lookups
CREATE INDEX IF NOT EXISTS idx_google_id ON members(google_id);
