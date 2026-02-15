-- V7: parent_bet_id에 Partial Unique Index 추가 (NULL 값 제외)
-- BET_SELL 타입만 parent_bet_id가 NOT NULL이므로 중복 방지

-- 기존 UNIQUE 제약조건 제거 (NULL 값도 포함되어 문제가 될 수 있음)
ALTER TABLE activities DROP CONSTRAINT IF EXISTS uk_activities_parent_bet_id;

-- Partial Unique Index 생성 (parent_bet_id가 NOT NULL인 행에만 적용)
CREATE UNIQUE INDEX IF NOT EXISTS idx_activities_parent_bet_id_not_null
ON activities (parent_bet_id)
WHERE parent_bet_id IS NOT NULL;
