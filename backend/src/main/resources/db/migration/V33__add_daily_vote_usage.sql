-- V33: 일별 투표 사용 현황 테이블
-- 목적: (member_id, usage_date) 기준 일일 투표 횟수 추적
--       최초 INSERT + 이후 UPSERT(ON DUPLICATE KEY UPDATE) 가능

CREATE TABLE IF NOT EXISTS daily_vote_usage (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    member_id    BIGINT   NOT NULL,
    usage_date   DATE     NOT NULL          COMMENT 'UTC 기준 날짜 (yyyy-MM-dd)',
    used_count   INT      NOT NULL DEFAULT 0 COMMENT '해당 날짜 투표 횟수',
    created_at   DATETIME(6) NOT NULL       COMMENT '최초 INSERT 시각 (UTC)',
    updated_at   DATETIME(6) NOT NULL       COMMENT '마지막 UPSERT 시각 (UTC)',
    PRIMARY KEY (id),
    CONSTRAINT uk_dvu_member_date UNIQUE (member_id, usage_date),
    CONSTRAINT fk_dvu_member      FOREIGN KEY (member_id) REFERENCES members (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='일별 회원 투표 사용 현황';

-- queue scan: 특정 날짜의 한도 초과 회원 조회
CREATE INDEX IF NOT EXISTS idx_dvu_usage_date_count
    ON daily_vote_usage (usage_date, used_count);
