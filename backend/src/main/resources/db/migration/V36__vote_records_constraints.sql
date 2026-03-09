-- V36: vote_records UNIQUE(member_id, question_id) 제약 추가
-- 목적: 질문당 1회 투표를 DB 레벨에서 최종 보장
-- 패턴: information_schema.STATISTICS 조회 후 존재하지 않을 때만 생성 (멱등성)

-- 기존 중복 데이터 여부 사전 확인을 위한 인덱스 존재 체크 후 생성
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'vote_records'
      AND INDEX_NAME   = 'uk_vr_member_question'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE vote_records ADD CONSTRAINT uk_vr_member_question UNIQUE (member_id, question_id)',
    'SELECT "Constraint uk_vr_member_question already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 단건 조회 성능을 위한 일반 인덱스 (member_id 기준)
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'vote_records'
      AND INDEX_NAME   = 'idx_vr_member_id'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_vr_member_id ON vote_records (member_id)',
    'SELECT "Index idx_vr_member_id already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
