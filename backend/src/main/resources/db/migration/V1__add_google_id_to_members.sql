-- Add google_id column to members table for Google OAuth integration
-- Migration: V1__add_google_id_to_members.sql
-- Date: 2026-02-09

ALTER TABLE members
ADD COLUMN google_id VARCHAR(255) UNIQUE NULL
AFTER email;

-- Add index for faster lookups
ALTER TABLE members
ADD INDEX idx_google_id (google_id);
