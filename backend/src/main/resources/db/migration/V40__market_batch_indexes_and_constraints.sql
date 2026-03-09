-- V40: market_open_batches / question_market_candidates 인덱스 추가
-- 멱등성: information_schema.STATISTICS 조회 후 존재하지 않을 때만 생성

-- (1) (batch_id, category, rank_in_category) — 카테고리별 Top3 조회 최적화
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_market_candidates'
      AND INDEX_NAME   = 'idx_qmc_batch_cat_rank'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_qmc_batch_cat_rank ON question_market_candidates (batch_id, category, rank_in_category)',
    'SELECT "Index idx_qmc_batch_cat_rank already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (2) (open_status) — 실패 건 재시도 조회 최적화
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_market_candidates'
      AND INDEX_NAME   = 'idx_qmc_open_status'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_qmc_open_status ON question_market_candidates (open_status)',
    'SELECT "Index idx_qmc_open_status already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (3) market_open_batches.status 인덱스 — 배치 상태별 조회 최적화
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'market_open_batches'
      AND INDEX_NAME   = 'idx_mob_status'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_mob_status ON market_open_batches (status)',
    'SELECT "Index idx_mob_status already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
