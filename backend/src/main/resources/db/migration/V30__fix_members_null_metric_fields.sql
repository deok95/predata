-- V30: members 테이블의 null 메트릭 컬럼 정리
-- 문제: legacy 데이터에서 NULL이 남아 JPA primitive/non-null 매핑 시 500 발생

-- 1) 기존 NULL 데이터 백필
UPDATE members SET accuracy_score = 0 WHERE accuracy_score IS NULL;
UPDATE members SET total_predictions = 0 WHERE total_predictions IS NULL;
UPDATE members SET correct_predictions = 0 WHERE correct_predictions IS NULL;
UPDATE members SET tier_weight = 1.00 WHERE tier_weight IS NULL;

-- 2) 스키마 제약 강화 (재발 방지)
ALTER TABLE members
    MODIFY COLUMN accuracy_score INT NOT NULL DEFAULT 0,
    MODIFY COLUMN total_predictions INT NOT NULL DEFAULT 0,
    MODIFY COLUMN correct_predictions INT NOT NULL DEFAULT 0,
    MODIFY COLUMN tier_weight DECIMAL(3,2) NOT NULL DEFAULT 1.00;
