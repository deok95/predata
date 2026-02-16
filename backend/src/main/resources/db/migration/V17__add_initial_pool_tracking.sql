-- 비대칭 초기 풀 추적을 위한 컬럼 추가
-- 투표 결과 기반 조건부 초기 배당 지원
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS initial_yes_pool BIGINT NOT NULL DEFAULT 500;

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS initial_no_pool BIGINT NOT NULL DEFAULT 500;

-- 기존 질문 데이터 정규화
UPDATE questions
SET initial_yes_pool = COALESCE(initial_yes_pool, 500),
    initial_no_pool = COALESCE(initial_no_pool, 500);
