-- Sprint 3: 시장 메타 스키마 확장
-- BET-009: Question 테이블에 시장 타입 및 정산 규칙 필드 추가

ALTER TABLE questions ADD COLUMN IF NOT EXISTS market_type VARCHAR(20) DEFAULT 'VERIFIABLE' NOT NULL;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS resolution_rule TEXT;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS resolution_source VARCHAR(500);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS resolve_at DATETIME;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS dispute_until DATETIME;

-- 기존 데이터에 대한 기본값 설정
UPDATE questions SET resolution_rule = '기본 정산 규칙' WHERE resolution_rule IS NULL;

-- resolution_rule을 NOT NULL로 변경
ALTER TABLE questions MODIFY COLUMN resolution_rule TEXT NOT NULL;
