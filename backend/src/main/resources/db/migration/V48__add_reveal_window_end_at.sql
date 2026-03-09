-- V48: VOTE_RESULT 질문의 reveal 윈도우 마감 시각 컬럼 추가
-- OBJECTIVE_RULE 질문은 NULL (commit-reveal 없음)
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS reveal_window_end_at DATETIME NULL
        COMMENT 'VOTE_RESULT 질문의 reveal 마감 시각 (NULL = OBJECTIVE_RULE 질문)';
