-- V35: 온체인 투표 릴레이 큐 테이블
-- 목적: 투표 결과를 온체인에 기록하는 relay 작업 큐
--       queue scan (PENDING 상태 조회) + retry scan (실패 재시도) 지원

CREATE TABLE IF NOT EXISTS onchain_vote_relays (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    vote_id       BIGINT       NOT NULL          COMMENT '원본 vote_records.id',
    member_id     BIGINT       NOT NULL,
    question_id   BIGINT       NOT NULL,
    choice        ENUM('YES','NO') NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                               COMMENT 'PENDING | SUBMITTED | CONFIRMED | FAILED',
    retry_count   INT          NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    max_retries   INT          NOT NULL DEFAULT 8 COMMENT '최대 재시도 허용 횟수 (app.relay.max-retry와 동일)',
    tx_hash       VARCHAR(66)  NULL              COMMENT '온체인 트랜잭션 해시 (0x 포함 최대 66자)',
    error_message VARCHAR(500) NULL              COMMENT '마지막 실패 오류 메시지',
    next_retry_at DATETIME(6)  NULL              COMMENT '다음 재시도 가능 시각 (UTC)',
    created_at    DATETIME(6)  NOT NULL          COMMENT '릴레이 생성 시각 (UTC)',
    updated_at    DATETIME(6)  NOT NULL          COMMENT '마지막 상태 변경 시각 (UTC)',
    PRIMARY KEY (id),
    CONSTRAINT uk_ovr_vote_id UNIQUE (vote_id),
    CONSTRAINT fk_ovr_member   FOREIGN KEY (member_id)   REFERENCES members (member_id),
    CONSTRAINT fk_ovr_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='온체인 투표 릴레이 큐';

-- queue scan: PENDING 상태의 미처리 릴레이 조회 (created_at 순)
CREATE INDEX IF NOT EXISTS idx_ovr_status_created
    ON onchain_vote_relays (status, created_at);

-- retry scan: FAILED이면서 next_retry_at이 지난 릴레이 조회
CREATE INDEX IF NOT EXISTS idx_ovr_status_retry
    ON onchain_vote_relays (status, retry_count, next_retry_at);
