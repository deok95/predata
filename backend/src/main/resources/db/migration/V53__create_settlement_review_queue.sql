-- V53: 자동정산 실패/보류 건 관리 큐
-- 재시도 스케줄러가 PENDING_RETRY 건을 주기적으로 재시도하고,
-- 한도 초과 시 NEEDS_MANUAL로 전환하여 관리자 수동 처리를 유도한다.

CREATE TABLE IF NOT EXISTS settlement_review_queue (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    question_id     BIGINT       NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING_RETRY'  COMMENT 'PENDING_RETRY | NEEDS_MANUAL | RESOLVED',
    reason_code     VARCHAR(64)  NOT NULL                           COMMENT 'SOURCE_UNAVAILABLE | NOT_FINISHED | RULE_PARSE_FAILED | CONFIDENCE_LOW | VOTE_REVEAL_PENDING | EXCEPTION',
    reason_detail   TEXT                                            COMMENT '상세 사유 (어댑터 반환 메시지 등)',
    retry_count     INT          NOT NULL DEFAULT 0,
    max_retry       INT          NOT NULL DEFAULT 3,
    next_retry_at   DATETIME(6)                                     COMMENT 'NULL이면 즉시 재시도 대상',
    last_tried_at   DATETIME(6),
    last_error      TEXT                                            COMMENT '마지막 재시도 실패 메시지',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE  KEY uq_srq_question  (question_id),
    INDEX         idx_srq_status_retry (status, next_retry_at),
    CONSTRAINT fk_srq_question FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
