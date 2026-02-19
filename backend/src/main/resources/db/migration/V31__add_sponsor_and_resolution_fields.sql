-- V31: Add sponsor and auto-resolution fields to questions table
-- Purpose: Support BRANDED category (sponsored questions) and AUTO resolution type

-- Add sponsor fields for BRANDED category questions
ALTER TABLE questions
    ADD COLUMN sponsor_name VARCHAR(100) NULL COMMENT 'Sponsor name for BRANDED questions';

ALTER TABLE questions
    ADD COLUMN sponsor_logo VARCHAR(500) NULL COMMENT 'Sponsor logo URL for BRANDED questions';

-- Add auto-resolution fields
ALTER TABLE questions
    ADD COLUMN resolution_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL or AUTO settlement type';

ALTER TABLE questions
    ADD COLUMN resolution_config TEXT NULL COMMENT 'JSON config for auto-resolution (matchId, asset, condition, etc)';

-- Add indexes for efficient filtering
CREATE INDEX idx_questions_resolution_type_status ON questions(resolution_type, status);
CREATE INDEX idx_questions_category ON questions(category);

-- Comments for context
-- resolution_type: 'MANUAL' (admin/vote-based settlement) or 'AUTO' (external data source)
-- resolution_config: JSON format examples:
--   Football: {"matchId":"12345"}
--   Price: {"asset":"BTC","condition":">=100000"}
-- Reuses existing resolutionSource field for source type (FOOTBALL_API, PRICE_API)
