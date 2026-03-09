-- V34: 질문별 투표 집계 테이블
-- 목적: question_id PK, yes/no/total 카운터 원자 집계
--       INSERT … ON DUPLICATE KEY UPDATE 로 원자 증감 보장

CREATE TABLE IF NOT EXISTS vote_summary (
    question_id  BIGINT NOT NULL,
    yes_count    BIGINT NOT NULL DEFAULT 0 COMMENT 'YES 투표 수',
    no_count     BIGINT NOT NULL DEFAULT 0 COMMENT 'NO 투표 수',
    total_count  BIGINT NOT NULL DEFAULT 0 COMMENT '전체 투표 수 (yes + no)',
    updated_at   DATETIME(6) NOT NULL      COMMENT '마지막 갱신 시각 (UTC)',
    PRIMARY KEY (question_id),
    CONSTRAINT fk_vs_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='질문별 투표 집계 (원자 카운터)';
