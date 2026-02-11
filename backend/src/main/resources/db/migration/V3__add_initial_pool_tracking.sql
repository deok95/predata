-- 비대칭 초기 풀 추적을 위한 컬럼 추가
-- 투표 결과 기반 조건부 초기 배당 지원
ALTER TABLE questions ADD COLUMN initial_yes_pool BIGINT DEFAULT 500;
ALTER TABLE questions ADD COLUMN initial_no_pool BIGINT DEFAULT 500;

-- 기존 질문들은 대칭 초기 풀(500:500)로 설정
UPDATE questions SET initial_yes_pool = 500, initial_no_pool = 500;
