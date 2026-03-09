-- V41: questions.category 데이터 정리 및 FK 추가
--
-- 목적:
--   1. 시스템 내부 카테고리(TRENDING 등) market_categories에 등록 (is_active=FALSE)
--      → FK를 만족하되 마켓 선별 대상에서 제외
--   2. market_categories에 없는 잘못된 category 값을 NULL로 초기화
--   3. FK 추가: questions.category → market_categories.code
--      ON UPDATE CASCADE: 카테고리 코드 변경 시 자동 전파
--      ON DELETE SET NULL: 카테고리 삭제 시 질문을 미분류로 처리
--
-- 전제: market_categories 시드(V39) 완료 상태

-- 1. 시스템 내부 카테고리 등록 (is_active=FALSE: 마켓 선별 제외, FK 만족용)
--    INSERT IGNORE → 멱등 (이미 존재하면 스킵)
INSERT IGNORE INTO market_categories (code, display_name, is_active, sort_order) VALUES
    ('TRENDING', '트렌딩 (자동생성 전용)', FALSE, 100);

-- 2. 유효하지 않은 카테고리를 NULL로 초기화
--    market_categories 전체 코드셋 기준 (is_active 무관)으로 검사
UPDATE questions
SET    category = NULL
WHERE  category IS NOT NULL
  AND  category NOT IN (
           SELECT code FROM market_categories
       );

-- 2. FK 추가 (멱등: 이미 존재하면 스킵)
SET @fk_exists = (
    SELECT COUNT(*)
    FROM   information_schema.TABLE_CONSTRAINTS
    WHERE  TABLE_SCHEMA    = DATABASE()
      AND  TABLE_NAME      = 'questions'
      AND  CONSTRAINT_NAME = 'fk_question_category'
      AND  CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE questions
         ADD CONSTRAINT fk_question_category
         FOREIGN KEY (category)
         REFERENCES market_categories(code)
         ON UPDATE CASCADE
         ON DELETE SET NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
